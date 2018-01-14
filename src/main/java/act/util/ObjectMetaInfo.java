package act.util;

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

import act.asm.Label;
import act.asm.MethodVisitor;
import act.asm.Opcodes;
import act.asm.Type;
import act.data.annotation.Data;
import org.osgl.$;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;

/**
 * Datastructure captures a class's meta information in related to {@link DataObjectEnhancer}.
 * The following info will be captured:
 * <ul>
 *     <li>Is the class annotated with {@link Data} annotation</li>
 *     <li>A list of {@link FieldMetaInfo} of all declared fields</li>
 * </ul>
 */
class ObjectMetaInfo implements Opcodes {

    /**
     * Datastructure captures a class's declared field meta info
     */
    static class FieldMetaInfo implements Opcodes {
        private String name;
        private boolean isTransient = false;
        private boolean equalForce = false;
        private boolean equalIgnore = false;
        private Type type;
        FieldMetaInfo(String name, Type type, boolean isTransient) {
            this.name = $.NPE(name);
            this.type = $.NPE(type);
            this.isTransient = isTransient;
        }
        void setEqualForce() {
            equalForce = true;
        }
        void setEqualIgnore() {
            equalIgnore = true;
        }
        void addEqualInstructions(Type host, MethodVisitor mv, Label jumpTo) {
            if (!eligible()) {
                return;
            }
            String typeDesc = type.getDescriptor();
            // ALOAD 0: this
            // ALOAD 2: that = (Type) obj (which is 1)
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(GETFIELD, host.getInternalName(), name, typeDesc);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, host.getInternalName(), name, typeDesc);
            String s = typeDesc;
            if (s.length() > 1) {
                s = OBJECT_TYPE.getDescriptor();
            }
            String op = "eq";
            if (typeDesc.startsWith("[")) {
                // array needs deep eq
                op = "eq2";
            }
            mv.visitMethodInsn(INVOKESTATIC, "org/osgl/Osgl", op, S.fmt("(%s%s)Z", s, s), false);
            mv.visitJumpInsn(IFEQ, jumpTo);
        }
        boolean addHashCodeInstruction(Type host, MethodVisitor mv) {
            if (!eligible()) {
                return false;
            }
            mv.visitVarInsn(ALOAD, 0); // load this pointer
            mv.visitFieldInsn(GETFIELD, host.getInternalName(), name, type.getDescriptor());
            convertFromPrimaryType(type, mv);
            return true;
        }
        private void convertFromPrimaryType(Type fieldType, MethodVisitor mv) {
            switch (fieldType.getSort()) {
                case Type.BOOLEAN:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",false);
                    break;
                case Type.BYTE:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;",false);
                    break;
                case Type.CHAR:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",false);
                    break;
                case Type.SHORT:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;",false);
                    break;
                case Type.INT:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",false);
                    break;
                case Type.LONG:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;",false);
                    break;
                case Type.FLOAT:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;",false);
                    break;
                case Type.DOUBLE:
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;",false);
                    break;
                default:
                    // do nothing
            }
        }
        boolean eligible() {
            return !equalIgnore && (!isTransient || equalForce);
        }
    }

    private Type superType;
    private Type type;
    private boolean callSuper;
    private List<FieldMetaInfo> fields = new ArrayList<>();
    private boolean hasEqualMethod = false;
    private boolean hasHashCodeMethod = false;
    private boolean hasToStringMethod = false;
    private boolean hasAutoObjectAnnotation = false;
    private static Type OBJECT_TYPE = Type.getType(Object.class);

    ObjectMetaInfo(Type type, Type superType) {
        this.type = $.NPE(type);
        if (null != superType && !OBJECT_TYPE.equals(superType)) {
            this.superType = superType;
        }
    }

    Type type() {
        return type;
    }

    Type superType() {
        return superType;
    }

    List<FieldMetaInfo> fields() {
        return fields;
    }

    FieldMetaInfo addField(String fieldName, Type fieldType, boolean isTransient) {
        FieldMetaInfo fi = new FieldMetaInfo(fieldName, fieldType, isTransient);
        fields.add(fi);
        return fi;
    }

    void requireCallSuper() {
        callSuper = true;
    }

    boolean shouldCallSuper() {
        return callSuper && null != superType;
    }

    void equalMethodFound() {
        hasEqualMethod = true;
    }

    void hashCodeMethodFound() {
        hasHashCodeMethod = true;
    }

    void toStringMethodFound() {
        hasToStringMethod = true;
    }

    void autoObjectAnnotationFound() {
        hasAutoObjectAnnotation = true;
    }

    boolean hasDataAnnotation() {
        return hasAutoObjectAnnotation;
    }

    boolean shouldGenerateEqualsMethod() {
        return hasAutoObjectAnnotation && !hasEqualMethod;
    }

    boolean shouldGenerateHashCodeMethod() {
        return hasAutoObjectAnnotation && !hasHashCodeMethod;
    }

}
