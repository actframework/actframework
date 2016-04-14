package act.mail.bytecode;

import act.ActComponent;
import act.app.App;
import act.asm.ClassVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.mail.meta.MailerClassMetaInfo;
import act.mail.meta.MailerClassMetaInfoHolder;
import act.mail.meta.SenderMethodMetaInfo;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;
import org.osgl.util.S;

/**
 * Enhance mailer class
 */
@ActComponent
public class MailerEnhancer extends AppByteCodeEnhancer<MailerEnhancer> {

    private MailerClassMetaInfoHolder classInfoHolder;
    private String className;

    public MailerEnhancer() {
        super(S.F.startsWith("act.").negate().or(S.F.startsWith("act.fsa")));
    }

    public MailerEnhancer(ClassVisitor cv, MailerClassMetaInfoHolder infoHolder) {
        super(_F.isMailer(infoHolder), cv);
        this.classInfoHolder = infoHolder;
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        this.classInfoHolder = app.classLoader();
        return super.app(app);
    }

    @Override
    protected Class<MailerEnhancer> subClass() {
        return MailerEnhancer.class;
    }

    public MailerEnhancer classInfoHolder(MailerClassMetaInfoHolder holder) {
        classInfoHolder = holder;
        predicate(_F.isMailer(holder));
        return this;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = Type.getObjectType(name).getClassName();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        SenderMethodMetaInfo info = methodInfo(name, access);
        if (null == info) {
            return mv;
        }
        logger.debug(">>>About to enhance mailer method: %s", name);
        return new SenderEnhancer(mv, info, access, name, desc, signature, exceptions);
    }

    private SenderMethodMetaInfo methodInfo(String name, int access) {
        if (isPublic(access) && !isConstructor(name)) {
            MailerClassMetaInfo ccInfo = classInfoHolder.mailerClassMetaInfo(className);
            if (null == ccInfo) {
                return null;
            }
            SenderMethodMetaInfo info = ccInfo.sender(name);
            if (null != info) {
                return info;
            }
            return ccInfo.sender(name);
        } else {
            return null;
        }
    }

    private boolean isTargetMethod(String name, int access) {
        return isPublic(access) && !isConstructor(name) && methodInfo(name, access) != null;
    }

    private static enum _F {
        ;

        private static final $.Predicate<String> isMailer(final MailerClassMetaInfoHolder infoSrc) {
            return new $.Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return infoSrc.mailerClassMetaInfo(s) != null;
                }
            };
        }
    }
}
