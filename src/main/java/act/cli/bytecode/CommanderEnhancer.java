package act.cli.bytecode;

import act.app.App;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommanderClassMetaInfo;
import act.cli.meta.CommanderClassMetaInfoHolder;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;

import java.util.HashSet;
import java.util.Set;

public class CommanderEnhancer extends AppByteCodeEnhancer<CommanderEnhancer> {

    private String className;
    private CommanderClassMetaInfoHolder infoBase;
    private CommanderClassMetaInfo metaInfo;

    public CommanderEnhancer() {
        super($.F.<String>yes());
    }

    @Override
    protected Class<CommanderEnhancer> subClass() {
        return CommanderEnhancer.class;
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        this.infoBase = app.classLoader();
        return super.app(app);
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = Type.getObjectType(name).getClassName();
        metaInfo = infoBase.commanderClassMetaInfo(className);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (null == metaInfo || isConstructor(name)) {
            return mv;
        }
        final CommandMethodMetaInfo methodInfo = metaInfo.command(name);
        if (null == methodInfo) {
            return mv;
        }
        if (isPublic(access) && !isConstructor(name)) {
            return new MethodVisitor(ASM5, mv) {
                private Set<Integer> skipNaming = new HashSet<Integer>();
                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                    if ("Ljavax/inject/Named;".equals(desc)) {
                        skipNaming.add(parameter);
                    }
                    return super.visitParameterAnnotation(parameter, desc, visible);
                }

                @Override
                public void visitEnd() {
                    int sz = methodInfo.paramCount();
                    for (int i = 0; i < sz; ++i) {
                        if (!skipNaming.contains(i)) {
                            String name = methodInfo.param(i).name();
                            AnnotationVisitor av = mv.visitParameterAnnotation(i, "Ljavax/inject/Named;", true);
                            av.visit("value", name);
                        }
                    }
                    super.visitEnd();
                }
            };
        }
        return mv;
    }
}
