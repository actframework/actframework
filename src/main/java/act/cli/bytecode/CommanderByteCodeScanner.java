package act.cli.bytecode;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.app.AppByteCodeScannerBase;
import act.asm.*;
import act.cli.CliDispatcher;
import act.cli.meta.*;
import act.cli.view.CliView;
import act.sys.meta.SessionVariableAnnoInfo;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.*;

/**
 * Scan Commander class bytecode
 */
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
        return null != dispatcher;
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
        classInfoBase().registerCommanderMetaInfo(classInfo);
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
            Type superType = Type.getObjectType(superName);
            classInfo.superType(superType);
            if (isAbstract(access)) {
                classInfo.setAbstract();
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            Type type = Type.getType(desc);
            if ($.eq(AsmTypes.CSV_VIEW_DEPRECATED.asmType(), type)) {
                classInfo.view(CliView.CSV);
                return av;
            } else if ($.eq(AsmTypes.CSV_VIEW.asmType(), type)) {
                classInfo.view(CliView.CSV);
                return av;
            } else if ($.eq(AsmTypes.TREE_VIEW.asmType(), type)) {
                classInfo.view(CliView.TREE);
                return av;
            } else if ($.eq(AsmTypes.TABLE_VIEW.asmType(), type)) {
                classInfo.view(CliView.TABLE);
                return av;
            } else if ($.eq(AsmTypes.JSON_VIEW_DEPRECATED.asmType(), type)) {
                classInfo.view(CliView.JSON);
                return av;
            } else if ($.eq(AsmTypes.JSON_VIEW.asmType(), type)) {
                classInfo.view(CliView.JSON);
                return av;
            } else if ($.eq(AsmTypes.CMD_PREFIX.asmType(), type)) {
                return new AnnotationVisitor(ASM5, av) {
                    @Override
                    public void visit(String name, Object value) {
                        classInfo.contextPath(S.string(value));
                        super.visit(name, value);
                    }
                };
            }
            return av;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            FieldVisitor fv = super.visitField(access, name, desc, signature, value);
            Type type = Type.getType(desc);
            return new CommanderFieldVisitor(fv, name, type);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!isEligibleMethod(access, name, desc)) {
                return mv;
            }
            return new CommandMethodVisitor(mv, access, name, desc, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            if (!classInfo.isAbstract()) {
                for (CommandMethodMetaInfo commandMethodMetaInfo : classInfo.commandList()) {
                    String prefix = classInfo.contextPath();
                    String commandName = commandMethodMetaInfo.commandName();
                    if (S.notBlank(prefix)) {
                        commandName = S.pathConcat(prefix, '.', commandName);
                    }
                    dispatcher.registerCommandHandler(commandName, commandMethodMetaInfo, classInfo);
                }
            }
            super.visitEnd();
        }

        private boolean isEligibleMethod(int access, String name, String desc) {
            return isPublic(access) && !isAbstract(access) && !isConstructor(name);
        }

        private class CommanderFieldVisitor extends FieldVisitor implements Opcodes {
            private String fieldName;
            private boolean readFileContent;
            private Type type;

            public CommanderFieldVisitor(FieldVisitor fv, String fieldName, Type type) {
                super(ASM5, fv);
                this.fieldName = fieldName;
                this.type = type;
            }

            @Override
            public void visitAttribute(Attribute attr) {
                super.visitAttribute(attr);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                Type type = Type.getType(desc);
                boolean isOptional = $.eq(type, AsmTypes.OPTIONAL.asmType());
                boolean isRequired = !isOptional && $.eq(type, AsmTypes.REQUIRED.asmType());
                readFileContent = !isOptional && !isRequired && $.eq(type, AsmTypes.READ_FILE_CONTENT.asmType());
                if (isOptional || isRequired) {
                    return new FieldOptionAnnotationVisitor(av, isOptional, fieldName, this.type);
                } else if ($.eq(type, AsmTypes.CLI_SESSION_ATTRIBUTE.asmType())) {
                    return new FieldCliSessionVariableAnnotationVisitor(av);
                }
                return av;
            }

            private class FieldCliSessionVariableAnnotationVisitor extends AnnotationVisitor implements Opcodes {
                private String sessionVariableName = fieldName;
                public FieldCliSessionVariableAnnotationVisitor(AnnotationVisitor av) {
                    super(ASM5, av);
                }

                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        String key = S.string(value);
                        if (S.blank(key)) {
                            sessionVariableName = fieldName;
                        } else {
                            sessionVariableName = key;
                        }
                    }
                    super.visit(name, value);
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    classInfo.addFieldSessionVariableAnnotInfo(fieldName, new SessionVariableAnnoInfo(sessionVariableName));
                }
            }

            private class FieldOptionAnnotationVisitor extends OptionAnnotationVisitorBase implements Opcodes {
                public FieldOptionAnnotationVisitor(AnnotationVisitor av, boolean optional, String fieldName, Type type) {
                    super(av, optional);
                    this.optionAnnoInfo = new FieldOptionAnnoInfo(fieldName, type, optional);
                }

                @Override
                public void visitEnd2() {
                    classInfo.addFieldOptionAnnotationInfo((FieldOptionAnnoInfo) optionAnnoInfo);
                }
            }

            @Override
            public void visitEnd() {
                if (readFileContent) {
                    FieldOptionAnnoInfo info = classInfo.fieldOptionAnnoInfo(fieldName);
                    if (null != info) {
                        info.setReadFileContent();
                    }
                }
                super.visitEnd();
            }
        }

        private class CommandMethodVisitor extends MethodVisitor implements Opcodes {

            private String methodName;
            private int access;
            private String desc;
            private String signature;
            private boolean requireScan;
            private CommandMethodMetaInfo methodInfo;
            private Map<Integer, ParamOptionAnnoInfo> optionAnnoInfoMap = new HashMap<>();
            private Map<Integer, String> cliSessionAttributeMap = new HashMap<>();
            private BitSet contextInfo = new BitSet();
            private boolean isStatic;
            private Map<Integer, Boolean> readFileContentFlags = new HashMap<>();
            private Set<Integer> skipNaming = new HashSet<Integer>();

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
                            if (S.eq("value", name) || S.eq("name", name)) {
                                String commandName = S.string(value);
                                if (S.empty(commandName)) {
                                    throw E.unexpected("command name cannot be empty");
                                }
                                methodInfo.commandName(commandName);
                            } else if (S.eq("help", name)) {
                                methodInfo.helpMsg(S.string(value));
                            }
                            super.visit(name, value);
                        }

                        @Override
                        public void visitEnum(String name, String desc, String value) {
                            if ("mode".equals(name)) {
                                methodInfo.mode(Act.Mode.valueOf(value));
                            }
                            super.visitEnum(name, desc, value);
                        }

                        @Override
                        public void visitEnd() {
                            if (S.blank(methodInfo.commandName())) {
                                throw new IllegalArgumentException("command name not defined");
                            }
                            super.visitEnd();
                        }
                    };
                } else if ($.eq(AsmTypes.CSV_VIEW_DEPRECATED.asmType(), type)) {
                    methodInfo.view(CliView.CSV);
                    return av;
                } else if ($.eq(AsmTypes.CSV_VIEW.asmType(), type)) {
                    methodInfo.view(CliView.CSV);
                    return av;
                } else if ($.eq(AsmTypes.TREE_VIEW.asmType(), type)) {
                    methodInfo.view(CliView.TREE);
                    return av;
                } else if ($.eq(AsmTypes.TABLE_VIEW.asmType(), type)) {
                    methodInfo.view(CliView.TABLE);
                    return av;
                } else if ($.eq(AsmTypes.JSON_VIEW_DEPRECATED.asmType(), type)) {
                    methodInfo.view(CliView.JSON);
                    return av;
                } else if ($.eq(AsmTypes.JSON_VIEW.asmType(), type)) {
                    methodInfo.view(CliView.JSON);
                    return av;
                } else if ($.eq(AsmTypes.PROPERTY_SPEC.asmType(), type)) {
                    final PropertySpec.MetaInfo propSpec = new PropertySpec.MetaInfo();
                    methodInfo.propertySpec(propSpec);
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            AnnotationVisitor av0 = super.visitArray(name);
                            if (S.eq("value", name)) {
                                return new AnnotationVisitor(ASM5, av0) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        propSpec.onValue(S.string(value));
                                        super.visit(name, value);
                                    }
                                };
                            } else if (S.eq("cli", name)) {
                                return new AnnotationVisitor(ASM5, av0) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        propSpec.onCli(S.string(value));
                                        super.visit(name, value);
                                    }
                                };
                            } else if (S.eq("http", name)) {
                                return new AnnotationVisitor(ASM5, av0) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        propSpec.onHttp(S.string(value));
                                        super.visit(name, value);
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
            public AnnotationVisitor visitParameterAnnotation(final int paramIndex, String desc, boolean visible) {
                AnnotationVisitor av = super.visitParameterAnnotation(paramIndex, desc, visible);
                Type type = Type.getType(desc);
                boolean isOptional = $.eq(type, AsmTypes.OPTIONAL.asmType());
                boolean isRequired = !isOptional && $.eq(type, AsmTypes.REQUIRED.asmType());

                if (isOptional || isRequired) {
                    if (optionAnnoInfoMap.containsKey(paramIndex)) {
                        throw E.unexpected("Option annotation already found on index %s", paramIndex);
                    }
                    return new ParamOptionAnnotationVisitor(av, paramIndex, isOptional);
                } else if ($.eq(type, AsmTypes.CONTEXT.asmType())) {
                    contextInfo.set(paramIndex);
                    return av;
                } else if ($.eq(type, AsmTypes.READ_FILE_CONTENT.asmType())) {
                    readFileContentFlags.put(paramIndex, true);
                    return av;
                } else if ($.eq(type, AsmTypes.CLI_SESSION_ATTRIBUTE.asmType())) {
                    return new AnnotationVisitor(ASM5, av) {
                        private String attributeKey = "";

                        @Override
                        public void visit(String name, Object value) {
                            if ("value".equals(name)) {
                                attributeKey = S.string(value);
                            }
                            super.visit(name, value);
                        }

                        @Override
                        public void visitEnd() {
                            cliSessionAttributeMap.put(paramIndex, attributeKey);
                            super.visitEnd();
                        }
                    };
                } else if ("Ljavax/inject/Named;".equals(desc)) {
                    skipNaming.add(paramIndex);
                    return av;
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
                    CommandParamMetaInfo param = methodInfo.param(i);
                    if (contextInfo.get(i)) {
                        param.setContext();
                    }
                    ParamOptionAnnoInfo option = optionAnnoInfoMap.get(i);
                    if (null != option) {
                        param.optionInfo(option);
                        methodInfo.addLead(option.lead1());
                        methodInfo.addLead(option.lead2());
                    }
                    if (null != readFileContentFlags.get(i)) {
                        param.setReadFileContent();
                    }
                    if (!skipNaming.contains(i)) {
                        String name = param.name();
                        //AnnotationVisitor av = visitParameterAnnotation(i, "Ljavax/inject/Named;", true);
                        //av.visit("value", name);
                    }
                }
                super.visitEnd();
            }

            private void markRequireScan() {
                this.requireScan = true;
            }

            private boolean requireScan() {
                return requireScan;
            }

            private class ParamOptionAnnotationVisitor extends OptionAnnotationVisitorBase implements Opcodes {
                protected int index;

                public ParamOptionAnnotationVisitor(AnnotationVisitor av, int index, boolean optional) {
                    super(av, optional);
                    this.index = index;
                    this.optionAnnoInfo = new ParamOptionAnnoInfo(index, optional);
                }

                @Override
                public void visitEnd2() {
                    optionAnnoInfoMap.put(index, (ParamOptionAnnoInfo) optionAnnoInfo);
                }
            }

        }
    }

    private static class OptionAnnotationVisitorBase extends AnnotationVisitor implements Opcodes {
        protected List<String> specs = new ArrayList<>();
        protected OptionAnnoInfoBase optionAnnoInfo;

        public OptionAnnotationVisitorBase(AnnotationVisitor av, boolean optional) {
            super(ASM5, av);
            // sub class to init "info" field here
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            AnnotationVisitor av = super.visitArray(name);
            if (S.eq("lead", name)) {
                return new AnnotationVisitor(ASM5, av) {
                    @Override
                    public void visit(String name, Object value) {
                        specs.add((String) value);
                        super.visit(name, value);
                    }
                };
            }
            return av;
        }

        @Override
        public void visit(String name, Object value) {
            if (S.eq("group", name)) {
                optionAnnoInfo.group((String) value);
            } else if (S.eq("defVal", name)) {
                optionAnnoInfo.defVal((String) value);
            } else if (S.eq("value", name) || S.eq("help", name)) {
                optionAnnoInfo.help((String) value);
            }
            super.visit(name, value);
        }

        @Override
        public void visitEnd() {
            if (!specs.isEmpty()) {
                optionAnnoInfo.spec(specs.toArray(new String[specs.size()]));
            }
            visitEnd2();
            super.visitEnd();
        }

        protected void visitEnd2() {
            // ...
        }
    }


}
