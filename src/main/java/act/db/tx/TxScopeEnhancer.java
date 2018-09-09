package act.db.tx;

/*-
 * #%L
 * ACT SQL Common Module
 * %%
 * Copyright (C) 2015 - 2018 ActFramework
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

import act.asm.*;
import act.asm.commons.AdviceAdapter;
import act.util.AppByteCodeEnhancer;
import org.osgl.util.S;

public class TxScopeEnhancer extends AppByteCodeEnhancer<TxScopeEnhancer> {

    private static final String DESC_TRANSACTIONAL = Type.getDescriptor(Transactional.class);
    private static final Type TYPE_TX_INFO = Type.getType(TxInfo.class);
    private static final Type TYPE_TX_SCOPE_HELPER = Type.getType(TxScopeHelper.class);
    private String className;
    private String methodName;

    public TxScopeEnhancer() {
        super(S.F.startsWith("act.").negate());
    }

    public TxScopeEnhancer(ClassVisitor cv) {
        super(S.F.startsWith("act.").negate(), cv);
    }

    @Override
    protected Class<TxScopeEnhancer> subClass() {
        return TxScopeEnhancer.class;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("<init>") || name.equals("<clinit>")) {
            // not enhancing constructors at the moment
            return mv;
        }
        methodName = name;
        return new AdviceAdapter(ASM5, mv, access, name, desc) {
            private boolean readOnly;
            private boolean txScoped;
            private int posTxInfo;
            private Label startFinally = new Label();


            @Override
            public void visitCode() {
                super.visitCode();
                if (txScoped) {
                    mv.visitLabel(startFinally);
                }
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                if (txScoped) {
                    Label endFinally = new Label();
                    mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
                    mv.visitLabel(endFinally);
                    onFinally(ATHROW);
                    mv.visitInsn(ATHROW);
                    mv.visitMaxs(maxStack, maxLocals);
                } else {
                    super.visitMaxs(maxStack, maxLocals);
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (DESC_TRANSACTIONAL.equals(desc)) {
                    txScoped = true;
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public void visit(String name, Object value) {
                            if ("readOnly".equals(name)) {
                                readOnly = Boolean.parseBoolean(value.toString());
                            }
                            super.visit(name, value);
                        }
                    };
                }
                return av;
            }

            @Override
            protected void onMethodEnter() {
                if (!txScoped) {
                    return;
                }
                posTxInfo = newLocal(TYPE_TX_INFO);
                mv.visitTypeInsn(NEW, TYPE_TX_INFO.getInternalName());
                mv.visitInsn(DUP);
                mv.visitInsn(readOnly ? ICONST_1 : ICONST_0);
                mv.visitMethodInsn(INVOKESPECIAL, TYPE_TX_INFO.getInternalName(), "<init>", "(Z)V", false);
                mv.visitVarInsn(ASTORE, posTxInfo);

                mv.visitVarInsn(ALOAD, posTxInfo);
                mv.visitMethodInsn(INVOKESTATIC, TYPE_TX_SCOPE_HELPER.getInternalName(), "enter", "("
                        + TYPE_TX_INFO.getDescriptor() + ")V", false);
            }

            protected final void onMethodExit(int opcode) {
                if (!txScoped) {
                    return;
                }
                if (opcode != ATHROW) {
                    onFinally(opcode);
                }
            }

            private void onFinally(int opcode) {
                if (opcode == RETURN) {
                    visitInsn(ACONST_NULL);

                } else if (opcode == ARETURN || opcode == ATHROW) {
                    dup();

                } else {
                    if (opcode == LRETURN || opcode == DRETURN) {
                        dup2();
                    } else {
                        dup();
                    }
                    box(Type.getReturnType(this.methodDesc));
                }
                visitIntInsn(SIPUSH, opcode);
                visitMethodInsn(INVOKESTATIC, TYPE_TX_SCOPE_HELPER.getInternalName(), "exit", "(Ljava/lang/Object;I)V", false);
            }
        };
    }


}
