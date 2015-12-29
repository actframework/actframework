package act.cli.bytecode;

import act.ActComponent;
import act.app.AppByteCodeScannerBase;
import act.asm.*;
import act.cli.CliDispatcher;
import act.cli.meta.*;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import act.util.DataView;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.ListBuilder;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;

/**
 * Scan Commander class bytecode
 */
@ActComponent
public class CommanderByteCodeScanner extends AppByteCodeScannerBase {

    private final static Logger logger = L.get(CommanderByteCodeScanner.class);
    private CliDispatcher dispatcher;
    private CommanderClassMetaInfo classInfo;
    private volatile CommanderClassMetaInfoManager classInfoBase;

    public CommanderByteCodeScanner() {
    }

    @Override
    protected void reset(String className) {
        classInfo = new CommanderClassMetaInfo();
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    protected void onAppSet() {
        dispatcher = app().cliDispatcher();
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        if (classInfo.hasCommand()) {
            classInfoBase().registerCommanderMetaInfo(classInfo);
        }
    }

    @Override
    public void allScanFinished() {
    }

    private CommanderClassMetaInfoManager classInfoBase() {
        if (null == classInfoBase) {
            synchronized (this) {
                if (null == classInfoBase) {
                    classInfoBase = app().classLoader().commanderClassMetaInfoManager();
                }
            }
        }
        return classInfoBase;
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            classInfo.className(name);
            String className = name.replace('/', '.');
            Type superType = Type.getObjectType(superName);
            classInfo.superType(superType);
            if (isAbstract(access)) {
                classInfo.setAbstract();
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if (AsmTypes.ACTION_CONTEXT_DESC.equals(desc)) {
                classInfo.ctxField(name, isPrivate(access));
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!isEligibleMethod(access, name, desc)) {
                return mv;
            }
            String className = classInfo.className();
            return new CommandMethodVisitor(mv, access, name, desc, signature, exceptions);
        }

        private boolean isEligibleMethod(int access, String name, String desc) {
            return isPublic(access) && !isAbstract(access) && !isConstructor(name);
        }

        private class StringArrayVisitor extends AnnotationVisitor {
            protected ListBuilder<String> strings = ListBuilder.create();

            public StringArrayVisitor(AnnotationVisitor av) {
                super(ASM5, av);
            }

            @Override
            public void visit(String name, Object value) {
                strings.add(value.toString());
                super.visit(name, value);
            }
        }

        private class CommandMethodVisitor extends MethodVisitor implements Opcodes {

            private String methodName;
            private int access;
            private String desc;
            private String signature;
            private boolean requireScan;
            private CommandMethodMetaInfo methodInfo;
            private Map<Integer, OptionAnnoInfo> optionAnnoInfo = C.newMap();
            private boolean isStatic;

            private int paramIdShift = 0;

            CommandMethodVisitor(MethodVisitor mv, int access, String methodName, String desc, String signature, String[] exceptions) {
                super(ASM5, mv);
                this.access = access;
                this.methodName = methodName;
                this.desc = desc;
                this.signature = signature;
                this.isStatic = AsmTypes.isStatic(access);
                methodInfo = new CommandMethodMetaInfo(classInfo);
                if (isStatic) {
                    methodInfo.invokeStaticMethod();
                } else {
                    methodInfo.invokeInstanceMethod();
                }
                methodInfo.methodName(methodName);
                methodInfo.returnType(Type.getReturnType(desc));
                Type[] argTypes = Type.getArgumentTypes(desc);
                for (int i = 0; i < argTypes.length; ++i) {
                    Type type = argTypes[i];
                    CommandParamMetaInfo param = new CommandParamMetaInfo().type(type);
                    methodInfo.addParam(param);
                }
            }

            @Override
            public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                if (!"this".equals(name)) {
                    int paramId = index;
                    if (null == methodInfo) {
                        methodInfo = new CommandMethodMetaInfo(classInfo);
                    }
                    if (!isStatic) {
                        paramId--;
                    }
                    paramId -= paramIdShift;
                    if (paramId < methodInfo.paramCount()) {
                        CommandParamMetaInfo param = methodInfo.param(paramId);
                        param.name(name);
                        if (Type.getType(long.class).equals(param.type()) || Type.getType(double.class).equals(param.type())) {
                            paramIdShift++;
                        }
                    }
                }
                super.visitLocalVariable(name, desc, signature, start, end, index);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                Type type = Type.getType(desc);
                if ($.eq(AsmTypes.COMMAND.asmType(), type)) {
                    markRequireScan();
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public void visit(String name, Object value) {
                            if (S.eq("value", name)) {
                                String commandName = S.string(value);
                                if (S.empty(commandName)) {
                                    throw E.unexpected("command name cannot be empty");
                                }
                                methodInfo.commandName(commandName);
                            }
                        }
                    };
                } else if ($.eq(AsmTypes.HELP_MSG.asmType(), type)) {
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public void visit(String name, Object value) {
                            if (S.eq("value", name)) {
                                methodInfo.helpMsg(S.string(value));
                            }
                        }
                    };
                } else if ($.eq(AsmTypes.DATA_VIEW.asmType(), type)) {
                    final DataView.MetaInfo info = new DataView.MetaInfo();
                    methodInfo.dataView(info);
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            AnnotationVisitor av0 = super.visitArray(name);
                            if (S.eq("value", name)) {
                                return new AnnotationVisitor(ASM5, av0) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        info.onValue(S.string(value));
                                    }
                                };
                            } else {
                                return av0;
                            }
                        }
                    };
                }
                return av;
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int paramIndex, String desc, boolean visible) {
                AnnotationVisitor av = super.visitParameterAnnotation(paramIndex, desc, visible);
                Type type = Type.getType(desc);
                boolean isOptional = $.eq(type, AsmTypes.OPTIONAL.asmType());
                boolean isRequired = $.eq(type, AsmTypes.REQUIRED.asmType());
                if (isOptional || isRequired) {
                    if (optionAnnoInfo.containsKey(paramIndex)) {
                        throw E.unexpected("Option annotation already found on index %s", paramIndex);
                    }
                    return new OptionAnnotationVisitor(av, paramIndex, isOptional);
                } else {
                    return av;
                }
            }

            @Override
            public void visitEnd() {
                if (!requireScan()) {
                    super.visitEnd();
                    return;
                }
                classInfo.addCommand(methodInfo);
                Type[] argTypes = Type.getArgumentTypes(desc);
                for (int i = 0; i < argTypes.length; ++i) {
                    Type type = argTypes[i];
                    CommandParamMetaInfo param = methodInfo.param(i);
                    OptionAnnoInfo option = optionAnnoInfo.get(i);
                    if (null != option) {
                        param.optionInfo(option);
                        methodInfo.addLead(option.lead1());
                        methodInfo.addLead(option.lead2());
                    }
                }
                dispatcher.registerCommandHandler(methodInfo.commandName(), methodInfo);
                super.visitEnd();
            }

            private void markRequireScan() {
                this.requireScan = true;
            }

            private boolean requireScan() {
                return requireScan;
            }

            private class OptionAnnotationVisitor extends AnnotationVisitor implements Opcodes {
                protected int index;
                protected List<String> values = C.newList();
                protected OptionAnnoInfo info;

                public OptionAnnotationVisitor(AnnotationVisitor av, int index, boolean optional) {
                    super(ASM5, av);
                    this.index = index;
                    this.info = new OptionAnnoInfo(index, optional);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    AnnotationVisitor av = super.visitArray(name);
                    if (S.eq("value", name)) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public void visit(String name, Object value) {
                                values.add((String) value);
                            }
                        };
                    }
                    return av;
                }

                @Override
                public void visit(String name, Object value) {
                    if (S.eq("group", name)) {
                        info.group((String) value);
                    } else if (S.eq("defVal", name)) {
                        info.defVal((String) value);
                    } else if (S.eq("help", name)) {
                        info.help((String) value);
                    }
                }

                @Override
                public void visitEnd() {
                    if (!values.isEmpty()) {
                        info.value(values.toArray(new String[values.size()]));
                    }
                    optionAnnoInfo.put(index, info);
                }
            }

        }
    }

}
