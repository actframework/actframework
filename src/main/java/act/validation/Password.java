package act.validation;

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
import act.app.App;
import act.app.AppByteCodeScannerBase;
import act.asm.*;
import act.internal.password.MultiplePasswordProvider;
import act.internal.password.PasswordMetaInfo;
import act.internal.password.PasswordProvider;
import act.meta.ClassMetaInfoManager;
import act.util.AppByteCodeEnhancer;
import act.util.ByteCodeVisitor;
import act.util.SimpleBean;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotated element must be a valid password.
 * Accepts `char[]` type.
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = PasswordHandler.class)
public @interface Password {

    String ASM_DESC = Type.getType(Password.class).getDescriptor();

    String DEFAULT_PATTERN = "__def__";

    String value() default DEFAULT_PATTERN;

    String message() default "{act.validation.constraints.Password.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    /**
     * Defines several {@link Password} annotations on the same element.
     *
     * @see Password
     */
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
    @Retention(RUNTIME)
    @Documented
    @interface List {
        Password[] value();
    }

    class ByteCodeScanner extends AppByteCodeScannerBase {
        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        @Override
        public ByteCodeVisitor byteCodeVisitor() {
            return new ByteCodeVisitor() {

                private String className;
                private ClassMetaInfoManager<PasswordMetaInfo> metaInfoManager;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    className = Type.getObjectType(name).getClassName();
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public FieldVisitor visitField(int access, final String name, final String desc, String signature, Object value) {
                    FieldVisitor fv = super.visitField(access, name, desc, signature, value);
                    return new FieldVisitor(ASM5, fv) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (Password.ASM_DESC.equals(desc)) {
                                manager().getOrCreate(className).addPasswordField(name, desc);
                            }
                            return super.visitAnnotation(desc, visible);
                        }
                    };
                }

                private ClassMetaInfoManager<PasswordMetaInfo> manager() {
                    if (null == metaInfoManager) {
                        metaInfoManager = Act.app().classMetaInfoManager(PasswordMetaInfo.class);
                    }
                    return metaInfoManager;
                }
            };
        }

        @Override
        public void scanFinished(String className) {
        }

    }

    class Enhancer extends AppByteCodeEnhancer<Password.Enhancer> {

        static final String DESC_CHAR_ARRAY = "([C)V";

        private Set<String> passwordFieldSetters = new HashSet<>();
        private String className;
        private String classInternalName;
        private ClassMetaInfoManager<PasswordMetaInfo> metaInfoManager;
        private PasswordMetaInfo metaInfo;
        private boolean eligible;
        private boolean intfAdded;
        private boolean isSinglePasswordProvider;

        public Enhancer() {
            super(S.F.startsWith("act.").negate());
        }

        public Enhancer(ClassVisitor cv) {
            super(S.F.startsWith("act.").negate(), cv);
        }

        @Override
        public AppByteCodeEnhancer app(App app) {
            metaInfoManager = app.classMetaInfoManager(PasswordMetaInfo.class);
            return super.app(app);
        }

        @Override
        protected Class<Password.Enhancer> subClass() {
            return Password.Enhancer.class;
        }

        @Override
        protected void reset() {
            classInternalName = null;
            className = null;
            metaInfo = null;
            eligible = false;
            intfAdded = false;
            isSinglePasswordProvider = false;
            passwordFieldSetters.clear();
            super.reset();
        }

        @Override
        public int priority() {
            return SimpleBean.ByteCodeEnhancer.PRIORITY + 1;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            classInternalName = name;
            className = Type.getObjectType(name).getClassName();
            metaInfo = metaInfoManager.get(className);
            eligible = null != metaInfo && metaInfo.hasPasswordField();
            if (eligible) {
                passwordFieldSetters.addAll(C.list(metaInfo.passwordFieldNames()));
                C.List<String> intf = null == interfaces ? C.<String>newList() : C.newListOf(interfaces);
                if (metaInfo.isSinglePasswordProvider()) {
                    isSinglePasswordProvider = true;
                    String intfName = Type.getType(PasswordProvider.class).getInternalName();
                    if (!intf.contains(intfName)) {
                        intf.add(intfName);
                        intfAdded = true;
                    }
                } else {
                    String intfName = Type.getType(MultiplePasswordProvider.class).getInternalName();
                    if (!intf.contains(intfName)) {
                        intf.add(intfName);
                        intfAdded = true;
                    }
                }
                super.visit(version, access, name, signature, superName, intf.toArray(new String[intf.size()]));
            } else {
                super.visit(version, access, name, signature, superName, interfaces);
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = new PasswordVerifierInvokeAdaptor(super.visitMethod(access, name, desc, signature, exceptions), metaInfo);
            if (!eligible || !DESC_CHAR_ARRAY.equals(desc)) {
                return mv;
            }
            final String fieldName = fieldNameFromSetter(name);
            return null == fieldName || !passwordFieldSetters.contains(fieldName) ? mv : new MethodVisitor(ASM5, mv) {
                @Override
                public void visitCode() {
                    visitSetterCode(fieldName, this);
                    passwordFieldSetters.remove(fieldName);
               }
            };
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (!eligible) {
                return;
            }
            for (final String field : passwordFieldSetters) {
                MethodVisitor mv = visitMethod(
                        ACC_PUBLIC,
                        setterName(field),
                        DESC_CHAR_ARRAY,
                        null,
                        null);
                mv.visitCode();
                visitSetterCode(field, mv);
            }
            if (intfAdded) {
                if (isSinglePasswordProvider) {
                    MethodVisitor mv = visitMethod(
                            ACC_PUBLIC,
                            "password",
                            "()[C",
                            null,
                            null);
                    mv.visitCode();
                    visitSinglePasswordProviderMethod(metaInfo.singlePasswordFieldName(), mv);
                } else {
                    MethodVisitor mv = visitMethod(
                            ACC_PUBLIC,
                            "password",
                            "(Ljava/lang/String;)[C",
                            null,
                            null);
                    visitMultiplePasswordProviderMethod(mv);
                }
            }
        }

        private String fieldNameFromSetter(String methodName) {
            int len = methodName.length();
            if (len > 3 && (methodName.startsWith("set"))) {
                return S.lowerFirst(methodName.substring(3));
            }
            return null;
        }

        private void visitSetterCode(final String field, final MethodVisitor mv) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, "act/Act", "crypto", "()Lact/crypto/AppCrypto;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "act/crypto/AppCrypto", "passwordHash", "([C)[C", false);
            mv.visitFieldInsn(PUTFIELD, classInternalName, field, "[C");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "fill", "([CC)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }

        private void visitSinglePasswordProviderMethod(final String field, final MethodVisitor mv) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, classInternalName, field, "[C");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        private void visitMultiplePasswordProviderMethod(final MethodVisitor mv) {
            for (String field : metaInfo.passwordFieldNames()) {
                mv.visitLdcInsn(field);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                Label l1 = new Label();
                mv.visitJumpInsn(IFEQ, l1);
                Label l2 = new Label();
                mv.visitLabel(l2);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, classInternalName, field, "[C");
                mv.visitInsn(ARETURN);
                mv.visitLabel(l1);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            mv.visitInsn(ARETURN);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "()V", false);
            mv.visitInsn(ATHROW);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }

        private static String setterName(String fieldName) {
            return "set" + S.capFirst(fieldName);
        }

        private static class PasswordVerifierInvokeAdaptor extends MethodVisitor {
            PasswordMetaInfo metaInfo;
            public PasswordVerifierInvokeAdaptor(MethodVisitor mv, PasswordMetaInfo metaInfo) {
                super(ASM5, mv);
                this.metaInfo = metaInfo;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (INVOKESTATIC == opcode && !itf && "verifyPassword".equals(name) && "act/validation/Password$Verifier".equals(owner)) {
                    if (null == metaInfo) {
                        throw AsmException.of("Cannot call Password.Verifier.verifyPassword on object without a @Password field");
                    }
                    if ("([CLjava/lang/Object;)Z".equals(desc)) {
                        if (!metaInfo.isSinglePasswordProvider()) {
                            throw AsmException.of("Cannot call Password.Verifier.verifyPassword on object with multiple @Password fields without field name parameter");
                        }
                        super.visitMethodInsn(opcode, owner, name, "([CLact/internal/password/PasswordProvider;)Z", itf);
                    } else if ("([CLjava/lang/Object;Ljava/lang/String;)Z".equals(desc)) {
                        if (metaInfo.isSinglePasswordProvider()) {
                            throw AsmException.of("It shall call Password.Verifier.verifyPassword on object with single @Password field without field name parameter");
                        }
                        super.visitMethodInsn(opcode, owner, name, "([CLact/internal/password/PasswordProvider;Ljava/lang/String;)Z", itf);
                    }
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        }
    }

    /**
     * Responsible for validating password string
     */
    interface Validator {
        /**
         * Check if a given password string is validate
         * @param password a password string
         * @return `true` if the password string is valid or `false` otherwise
         */
        boolean isValid(char[] password);
    }

    class Verifier {
        public static boolean verifyPassword(char[] passwordText, Object passwordHolder) {
            E.illegalStateIfNot(passwordHolder instanceof PasswordProvider, "passwordHolder is not a PasswordProvider");
            return verifyPassword(passwordText, ((PasswordProvider) passwordHolder));
        }
        public static boolean verifyPassword(char[] passwordText, PasswordProvider passwordHolder) {
            boolean ok = Act.crypto().verifyPassword(passwordText, passwordHolder.password());
            Arrays.fill(passwordText, '\0');
            return ok;
        }
        public static boolean verifyPassword(char[] passwordText, Object passwordHolder, String fieldName) {
            E.illegalStateIfNot(passwordHolder instanceof MultiplePasswordProvider, "passwordHolder is not a MultiplePasswordProvider");
            return verifyPassword(passwordText, (MultiplePasswordProvider) passwordHolder, fieldName);
        }
        public static boolean verifyPassword(char[] passwordText, MultiplePasswordProvider passwordHolder, String fieldName) {
            boolean ok = Act.crypto().verifyPassword(passwordText, passwordHolder.password(fieldName));
            Arrays.fill(passwordText, '\0');
            return ok;
        }
    }
}
