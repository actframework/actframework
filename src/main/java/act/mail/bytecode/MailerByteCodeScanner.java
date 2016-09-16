package act.mail.bytecode;

import act.ActComponent;
import act.app.AppByteCodeScannerBase;
import act.asm.*;
import act.asm.signature.SignatureReader;
import act.asm.signature.SignatureVisitor;
import act.controller.meta.HandlerParamMetaInfo;
import act.controller.meta.ParamAnnoInfoTrait;
import act.mail.Mailer;
import act.mail.meta.MailerClassMetaInfo;
import act.mail.meta.MailerClassMetaInfoManager;
import act.mail.meta.SenderMethodMetaInfo;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import act.util.GeneralAnnoInfo;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.List;
import java.util.Map;

/**
 * Mailer scanner implementation
 */
@ActComponent
public class MailerByteCodeScanner extends AppByteCodeScannerBase {

    private final static Logger logger = L.get(MailerByteCodeScanner.class);
    private MailerClassMetaInfo classInfo;
    private volatile MailerClassMetaInfoManager classInfoBase;

    @Override
    protected boolean shouldScan(String className) {
        classInfo = new MailerClassMetaInfo();
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        classInfoBase().registerMailerMetaInfo(classInfo);
    }

    private MailerClassMetaInfoManager classInfoBase() {
        if (null == classInfoBase) {
            synchronized (this) {
                if (null == classInfoBase) {
                    classInfoBase = app().classLoader().mailerClassMetaInfoManager();
                }
            }
        }
        return classInfoBase;
    }

    public static boolean isMailerAnno(String desc) {
        return Type.getType(Mailer.class).getDescriptor().equals(desc);
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            logger.trace("Scanning %s", name);
            classInfo.className(name);
            if (isAbstract(access)) {
                classInfo.setAbstract();
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if (classInfo.isMailer() && AsmTypes.ACTION_CONTEXT_DESC.equals(desc)) {
                classInfo.ctxField(name, isPrivate(access));
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (isMailerAnno(desc)) {
                classInfo.isMailer(true);
                return new MailerAnnotationVisitor(av);
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!classInfo.isMailer() || !isEligibleMethod(access, name, desc)) {
                return mv;
            }
            return new SenderMethodVisitor(mv, access, name, desc, signature, exceptions);
        }

        private boolean isEligibleMethod(int access, String name, String desc) {
            return isPublic(access) && !isAbstract(access) && !isConstructor(name) && name.startsWith("send");
        }

        private class MailerAnnotationVisitor extends AnnotationVisitor {
            MailerAnnotationVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public void visit(String name, Object value) {
                if ("value".equals(name)) {
                    classInfo.configId(value.toString());
                }
            }
        }

        private class SenderMethodVisitor extends MethodVisitor implements Opcodes {

            private String methodName;
            private int access;
            private String desc;
            private String signature;
            private boolean requireScan;
            private SenderMethodMetaInfo methodInfo;
            private Map<Integer, List<ParamAnnoInfoTrait>> paramAnnoInfoList = C.newMap();
            private Map<Integer, List<GeneralAnnoInfo>> genericParamAnnoInfoList = C.newMap();

            SenderMethodVisitor(MethodVisitor mv, int access, String methodName, String desc, String signature, String[] exceptions) {
                super(ASM5, mv);
                this.access = access;
                this.methodName = methodName;
                this.desc = desc;
                this.signature = signature;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (isMailerAnno(desc)) {
                    if (null == methodInfo) {
                        methodInfo = new SenderMethodMetaInfo(classInfo);
                        classInfo.addSender(methodInfo);
                    }
                    return new SenderAnnotationVisitor(av);
                }
                return av;
            }

            @Override
            public void visitEnd() {
                if (null == methodInfo) {
                    methodInfo = new SenderMethodMetaInfo(classInfo);
                    classInfo.addSender(methodInfo);
                }
                final SenderMethodMetaInfo info = methodInfo;
                info.name(methodName);
                boolean isStatic = AsmTypes.isStatic(access);
                if (isStatic) {
                    info.invokeStaticMethod();
                } else {
                    info.invokeInstanceMethod();
                }
                info.returnType(Type.getReturnType(desc));
                Type[] argTypes = Type.getArgumentTypes(desc);
                boolean ctxByParam = false;
                for (int i = 0; i < argTypes.length; ++i) {
                    Type type = argTypes[i];
                    HandlerParamMetaInfo param = new HandlerParamMetaInfo().type(type);
                    List<ParamAnnoInfoTrait> paraAnnoList = paramAnnoInfoList.get(i);
                    if (null != paraAnnoList) {
                        for (ParamAnnoInfoTrait trait : paraAnnoList) {
                            trait.attachTo(param);
                        }
                    }
                    List<GeneralAnnoInfo> list = genericParamAnnoInfoList.get(i);
                    if (null != list) {
                        param.addGeneralAnnotations(list);
                    }
                    info.addParam(param);
                }
                if (null != signature) {
                    SignatureReader sr = new SignatureReader(signature);
                    final $.Var<Integer> id = new $.Var<>(-1);
                    sr.accept(new SignatureVisitor(ASM5) {

                        boolean startParsing;

                        @Override
                        public SignatureVisitor visitParameterType() {
                            id.set(id.get() + 1);
                            return this;
                        }

                        @Override
                        public SignatureVisitor visitTypeArgument(char wildcard) {
                            if (wildcard == '=') {
                                startParsing = true;
                            }
                            return this;
                        }

                        @Override
                        public void visitClassType(String name) {
                            if (startParsing) {
                                Type type = Type.getObjectType(name);
                                int n = id.get();
                                if (n < 0) {
                                    info.returnComponentType(type);
                                } else {
                                    info.param(n).componentType(type);
                                }
                            }
                            startParsing = false;
                        }
                    });
                }
                super.visitEnd();
            }

            private class SenderAnnotationVisitor extends AnnotationVisitor implements Opcodes {

                public SenderAnnotationVisitor(AnnotationVisitor av) {
                    super(ASM5, av);
                }

                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        methodInfo.configId(value.toString());
                    }
                }

            }

        }
    }

}
