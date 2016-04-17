package act.event.bytecode;

import act.ActComponent;
import act.app.AppByteCodeScannerBase;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.event.EventBus;
import act.event.On;
import act.event.meta.SimpleEventListenerMetaInfo;
import act.util.AsmTypes;
import act.util.Async;
import act.util.ByteCodeVisitor;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

@ActComponent
public class SimpleEventListenerByteCodeScanner extends AppByteCodeScannerBase {

    private List<SimpleEventListenerMetaInfo> metaInfoList = C.newList();

    @Override
    protected void reset(String className) {
        metaInfoList.clear();
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        if (!metaInfoList.isEmpty()) {
            EventBus eventBus = app().eventBus();
            for (SimpleEventListenerMetaInfo metaInfo : metaInfoList) {
                for (String event : metaInfo.events()) {
                    if (metaInfo.isAsync()) {
                        eventBus.bindAsync(event, new ReflectedSimpleEventListener(metaInfo.className(), metaInfo.methodName(), metaInfo.paramTypes()));
                    } else {
                        eventBus.bind(event, new ReflectedSimpleEventListener(metaInfo.className(), metaInfo.methodName(), metaInfo.paramTypes()));
                    }
                }
            }
        }
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {

        private String className;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = Type.getObjectType(name).getClassName();
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            final List<String> paramTypes = C.newList();
            Type returnType = Type.getReturnType(desc);
            final boolean isVoid = "V".equals(returnType.toString());
            final boolean isPublicNotAbstract = AsmTypes.isPublicNotAbstract(access);
            Type[] arguments = Type.getArgumentTypes(desc);
            if (null != arguments) {
                for (Type type : arguments) {
                    paramTypes.add(type.getClassName());
                }
            }
            final String methodName = name;
            return new MethodVisitor(ASM5, mv) {

                private List<String> events = C.newList();

                private boolean isAsync;

                private String asyncMethodName = null;

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    AnnotationVisitor av = super.visitAnnotation(desc, visible);
                    String className = Type.getType(desc).getClassName();
                    if (On.class.getName().equals(className)) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                AnnotationVisitor av0 = super.visitArray(name);
                                if ("value".equals(name)) {
                                    return new AnnotationVisitor(ASM5, av0) {
                                        @Override
                                        public void visit(String name, Object value) {
                                            super.visit(name, value);
                                            events.add(S.string(value).intern());
                                        }
                                    };
                                } else {
                                    return av0;
                                }
                            }

                            @Override
                            public void visit(String name, Object value) {
                                if ("async".equals(name)) {
                                    isAsync = Boolean.parseBoolean(S.string(value));
                                }
                            }
                        };
                    } else {
                        if (Async.class.getName().equals(className)) {
                            if (!isVoid) {
                                logger.warn("Error found in method %s.%s: @Async annotation cannot be used with method that has return type", className, methodName);
                            } else if (!isPublicNotAbstract) {
                                logger.warn("Error found in method %s.%s: @Async annotation cannot be used with method that are not public or abstract method", className, methodName);
                            } else {
                                asyncMethodName = Async.MethodNameTransformer.transform(methodName);
                            }
                        }
                        return av;
                    }
                }

                @Override
                public void visitEnd() {
                    if (!events.isEmpty()) {
                        SimpleEventListenerMetaInfo metaInfo = new SimpleEventListenerMetaInfo(events, className, methodName, asyncMethodName, paramTypes, isAsync);
                        metaInfoList.add(metaInfo);
                    }
                    super.visitEnd();
                }
            };
        }
    }
}
