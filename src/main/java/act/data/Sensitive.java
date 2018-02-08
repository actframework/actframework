package act.data;

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

import act.asm.*;
import act.util.AppByteCodeEnhancer;
import act.util.SimpleBean;
import org.osgl.util.S;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

/**
 * Mark a **String** typed field is sensitive.
 * DB plugin should sense any field with this annotation so that
 * sensitive data be encrypted when stored into database and
 * decrypted after retrieving from database.
 *
 * If the framework found fields marked with `@Sensitive` it
 * will generate getter and setter for the field, if there are
 * already getter and setter defined, the method will be
 * overwritten.
 *
 * **Note** if a non String typed field marked with
 * `@Sensitive` nothing will be done for that field,
 * however framework will log a warn message.
 *
 * The logic of a sensitive getter/setter:
 *
 * ```java
 * {@literal @}Sensitive
 * private String sensitiveData;
 * // generated or overwritten getter
 * public String getSensitiveData(String data) {
 *     return Act.crypto().decrypt(data);
 * }
 * public void setSenstiveData(String data) {
 *     this.data = Act.crypto().encrypt(data);
 * }
 * ```
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sensitive {

    class Enhancer extends AppByteCodeEnhancer<Enhancer> {

        private static final String DESC_SENSITIVE = Type.getType(Sensitive.class).getDescriptor();
        private static final String DESC_STRING = Type.getType(String.class).getDescriptor();
        private static final String GETTER_DESC = S.concat("()", DESC_STRING);
        private static final String SETTER_DESC = S.concat("(", DESC_STRING, ")V");

        private Set<String> sensitiveFieldsForGetter = new HashSet<>();
        private Set<String> sensitiveFieldsForSetter = new HashSet<>();

        private String classInternalName;

        public Enhancer() {
            super(S.F.startsWith("act.").negate());
        }

        public Enhancer(ClassVisitor cv) {
            super(S.F.startsWith("act.").negate(), cv);
        }

        @Override
        protected Class<Enhancer> subClass() {
            return Enhancer.class;
        }

        @Override
        public int priority() {
            return SimpleBean.ByteCodeEnhancer.PRIORITY + 1;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            classInternalName = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, final String name, final String fieldDesc, String signature, Object value) {
            FieldVisitor fv = super.visitField(access, name, fieldDesc, signature, value);
            boolean isStatic = ((access & ACC_STATIC) != 0);
            return isStatic || S.neq(DESC_STRING, fieldDesc) ? fv : new FieldVisitor(ASM5, fv) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (DESC_SENSITIVE.equals(desc)) {
                        sensitiveFieldsForGetter.add(name);
                        sensitiveFieldsForSetter.add(name);
                    }
                    return super.visitAnnotation(desc, visible);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            final String fieldName = fieldNameFromGetterOrSetter(name);
            final boolean isSetter = null != fieldName && name.startsWith("s");
            Set<String> backlog = isSetter ? sensitiveFieldsForSetter : sensitiveFieldsForGetter;
            return null == fieldName || !backlog.contains(fieldName) ? mv : new MethodVisitor(ASM5, mv) {
                @Override
                public void visitCode() {
                    if (isSetter) {
                        visitSetterCode(fieldName, this);
                        sensitiveFieldsForSetter.remove(fieldName);
                    } else {
                        visitGetterCode(fieldName, this);
                        sensitiveFieldsForGetter.remove(fieldName);
                    }
                }
            };
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            for (final String field: sensitiveFieldsForGetter) {
                MethodVisitor mv = visitMethod(
                        ACC_PUBLIC,
                        getterName(field),
                        GETTER_DESC,
                        null,
                        null);
                mv.visitCode();
                visitGetterCode(field, mv);
            }
            for (final String field : sensitiveFieldsForSetter) {
                MethodVisitor mv = visitMethod(
                        ACC_PUBLIC,
                        setterName(field),
                        SETTER_DESC,
                        null,
                        null);
                mv.visitCode();
                visitSetterCode(field, mv);
            }
        }

        private String fieldNameFromGetterOrSetter(String methodName) {
            int len = methodName.length();
            if (len > 3 && (methodName.startsWith("get") || methodName.startsWith("set"))) {
                return S.lowerFirst(methodName.substring(3));
            }
            return null;
        }

        private void visitSetterCode(final String field, final MethodVisitor mv) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, "act/Act", "crypto", "()Lact/crypto/AppCrypto;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "act/crypto/AppCrypto", "encrypt", "(Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitFieldInsn(PUTFIELD, classInternalName, field, DESC_STRING);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }

        private void visitGetterCode(final String field, final MethodVisitor mv) {
            mv.visitMethodInsn(INVOKESTATIC, "act/Act", "crypto", "()Lact/crypto/AppCrypto;", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, classInternalName, field, "Ljava/lang/String;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "act/crypto/AppCrypto", "decrypt", "(Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }

        private static String getterName(String fieldName) {
            return S.concat("get", S.capFirst(fieldName));
        }

        private static String setterName(String fieldName) {
            return "set" + S.capFirst(fieldName);
        }
    }
}
