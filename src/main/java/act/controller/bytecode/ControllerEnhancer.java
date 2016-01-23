package act.controller.bytecode;

import act.ActComponent;
import act.app.App;
import act.asm.ClassVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.ControllerClassMetaInfoHolder;
import act.controller.meta.HandlerMethodMetaInfo;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;

/**
 * Enhance controllers (classes with either request handler method or
 * interceptor methods)
 */
@ActComponent
public class ControllerEnhancer extends AppByteCodeEnhancer<ControllerEnhancer> {
    private ControllerClassMetaInfoHolder classInfoHolder;
    private String className;

    public ControllerEnhancer() {
        super(null);
    }

    public ControllerEnhancer(ClassVisitor cv, ControllerClassMetaInfoHolder infoHolder) {
        super(_F.isController(infoHolder), cv);
        this.classInfoHolder = infoHolder;
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        this.classInfoHolder = app.classLoader();
        return super.app(app);
    }

    @Override
    protected Class<ControllerEnhancer> subClass() {
        return ControllerEnhancer.class;
    }

    public ControllerEnhancer classInfoHolder(ControllerClassMetaInfoHolder holder) {
        classInfoHolder = holder;
        predicate(_F.isController(holder));
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
        HandlerMethodMetaInfo info = methodInfo(name, access);
        if (null == info) {
            return mv;
        }
        logger.debug(">>>About to enhance handler: %s", name);
        return new HandlerEnhancer(mv, info, access, name, desc, signature, exceptions);
    }

    private HandlerMethodMetaInfo methodInfo(String name, int access) {
        if (isPublic(access) && !isConstructor(name)) {
            ControllerClassMetaInfo ccInfo = classInfoHolder.controllerClassMetaInfo(className);
            if (null == ccInfo) {
                return null;
            }
            HandlerMethodMetaInfo info = ccInfo.action(name);
            if (null != info) {
                return info;
            }
            return ccInfo.handler(name);
        } else {
            return null;
        }
    }

    private boolean isTargetMethod(String name, int access) {
        return isPublic(access) && !isConstructor(name) && methodInfo(name, access) != null;
    }

    private static enum _F {
        ;

        private static final $.Predicate<String> isController(final ControllerClassMetaInfoHolder infoSrc) {
            return new $.Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return infoSrc.controllerClassMetaInfo(s) != null;
                }
            };
        }
    }
}
