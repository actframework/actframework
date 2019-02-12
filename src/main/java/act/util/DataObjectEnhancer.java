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

import act.Act;
import act.asm.*;
import act.data.annotation.Data;
import org.osgl.$;
import org.osgl.util.*;

import java.util.List;
import java.util.Map;

/**
 * A tool to enhance a object by generating common {@link Object}
 * methods, e.g. {@link Object#equals(Object)}
 */
public class DataObjectEnhancer extends AppByteCodeEnhancer<DataObjectEnhancer> {

    private ObjectMetaInfo metaInfo;

    public DataObjectEnhancer() {
    }

    protected DataObjectEnhancer(ClassVisitor cv) {
        super($.F.<String>yes(), cv);
    }

    @Override
    protected Class<DataObjectEnhancer> subClass() {
        return DataObjectEnhancer.class;
    }

    @Override
    protected void reset() {
        this.metaInfo = null;
        super.reset();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        Type type = Type.getObjectType(name);
        Type superType = null == superName ? null : Type.getObjectType(superName);
        metaInfo = new ObjectMetaInfo(type, superType);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private static final String EQ_IGNORE = Type.getType(EqualIgnore.class).getDescriptor();
    private static final String EQ_FORCE = Type.getType(EqualField.class).getDescriptor();

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        boolean isStatic = ((access & ACC_STATIC) != 0);
        if (isStatic) {
            return fv;
        }
        if (metaInfo.hasDataAnnotation()) {
            boolean isTransient = ((access & ACC_TRANSIENT) != 0);
            Type fieldType = Type.getType(desc);
            final ObjectMetaInfo.FieldMetaInfo fi = metaInfo.addField(name, fieldType, isTransient);
            return new FieldVisitor(ASM5, fv) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (EQ_IGNORE.equals(desc)) {
                        fi.setEqualIgnore();
                    } else if (EQ_FORCE.equals(desc)) {
                        fi.setEqualForce();
                    }
                    return super.visitAnnotation(desc, visible);
                }
            };
        }
        return fv;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor def = super.visitAnnotation(desc, visible);
        if (Type.getType(Data.class).getDescriptor().equals(desc)) {
            metaInfo.autoObjectAnnotationFound();
            return new AnnotationVisitor(ASM5, def) {
                @Override
                public void visit(String name, Object value) {
                    if ("callSuper".equals(name)) {
                        String s = String.valueOf(value);
                        if (Boolean.parseBoolean(s)) {
                            metaInfo.requireCallSuper();
                        }
                    }
                    super.visit(name, value);
                }
            };
        }
        return def;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (metaInfo.hasDataAnnotation()) {
            if (S.eq("hashCode", name) && S.eq("()I", desc)) {
                metaInfo.hashCodeMethodFound();
            } else if (S.eq("equals", name) && S.eq("(Ljava/lang/Object;)Z", desc)) {
                metaInfo.equalMethodFound();
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (metaInfo.shouldGenerateEqualsMethod()) {
            generateEqualsMethod();
        }
        if (metaInfo.shouldGenerateHashCodeMethod()) {
            generateHashCodeMethod();
        }
        super.visitEnd();
    }

    private void generateEqualsMethod() {
        MethodVisitor mv = equalsMethodBegin(this);
        // check if obj == this
        mv.visitVarInsn(ALOAD, 1); // obj
        mv.visitVarInsn(ALOAD, 0); // this
        Label lbl_continue_with_instanceof_check = new Label();
        mv.visitJumpInsn(IF_ACMPNE, lbl_continue_with_instanceof_check);
        mv.visitInsn(ICONST_1); // true
        mv.visitInsn(IRETURN); // return
        mv.visitLabel(lbl_continue_with_instanceof_check);
        // check if obj instance of the type
        mv.visitVarInsn(ALOAD, 1); // obj
        Type host = metaInfo.type();
        String hostInternalName = host.getInternalName();
        mv.visitTypeInsn(INSTANCEOF, hostInternalName);
        Label lbl_exit_with_false = new Label();
        mv.visitJumpInsn(IFEQ, lbl_exit_with_false); // if not instance of then return false
        // do type cast
        mv.visitVarInsn(ALOAD, 1); // obj
        mv.visitTypeInsn(CHECKCAST, hostInternalName);
        mv.visitVarInsn(ASTORE, 2); // store cast object to that
        // should we check super result?
        if (shouldCallSuperForEquals()) {
            mv.visitVarInsn(ALOAD, 0); // load this
            mv.visitVarInsn(ALOAD, 2); // load that
            mv.visitMethodInsn(INVOKESPECIAL, metaInfo.superType().getInternalName(), "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFEQ, lbl_exit_with_false);
        }
        // call $.eq(that.f1, this.f1)
        List<ObjectMetaInfo.FieldMetaInfo> fields = metaInfo.fields();
        for (ObjectMetaInfo.FieldMetaInfo fi : fields) {
            fi.addEqualInstructions(host, mv, lbl_exit_with_false);
        }
        mv.visitInsn(ICONST_1); // true
        mv.visitInsn(IRETURN);
        mv.visitLabel(lbl_exit_with_false);
        mv.visitInsn(ICONST_0); // false
        mv.visitInsn(IRETURN);
        equalsMethodEnd(mv);
    }

    private int fieldCount(List<ObjectMetaInfo.FieldMetaInfo> fields) {
        int cnt = 0;
        for (ObjectMetaInfo.FieldMetaInfo field : fields) {
            if (field.eligible()) {
                cnt++;
            }
        }
        return cnt;
    }

    private void generateHashCodeMethod() {
        MethodVisitor mv = hashCodeMethodBegin(this);
        Type host = metaInfo.type();
        List<ObjectMetaInfo.FieldMetaInfo> fields = metaInfo.fields();
        int fieldCount = fieldCount(fields);
        boolean shouldCallSuper = shouldCallSuper(fieldCount);
        if (shouldCallSuper) {
            fieldCount++;
        }
        if (fieldCount < 6) {
            int cnt = 0;
            for (ObjectMetaInfo.FieldMetaInfo fi : fields) {
                boolean added = fi.addHashCodeInstruction(host, mv);
                if (added) {
                    cnt++;
                }
            }
            if (shouldCallSuper) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, metaInfo.superType().getInternalName(), "hashCode", "()I", false);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                cnt++;
            }
            hashCodeMethodEnd(mv, cnt);
        } else {
            mv.visitTypeInsn(NEW, "java/util/ArrayList");
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, fieldCount);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);
            mv.visitVarInsn(ASTORE, 1);
            for (ObjectMetaInfo.FieldMetaInfo fi : fields) {
                if (!fi.eligible()) {
                    continue;
                }
                mv.visitVarInsn(ALOAD, 1);
                fi.addHashCodeInstruction(host, mv);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(POP);
            }
            if (shouldCallSuper) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, metaInfo.superType().getInternalName(), "hashCode", "()I", false);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            }
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "org/osgl/$", "hc", "(Ljava/lang/Object;)I", false);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    private boolean shouldCallSuperForEquals() {
        if (null == metaInfo.superType()) {
            return false;
        }
        List<ObjectMetaInfo.FieldMetaInfo> fields = metaInfo.fields();
        int fieldCount = fieldCount(fields);
        return shouldCallSuper(fieldCount);
    }

    private boolean shouldCallSuper(int fieldCount) {
        if (null == metaInfo.superType()) {
            return false;
        }
        if (0 == fieldCount) {
            return true;
        }
        if (metaInfo.shouldCallSuper()) {
            return true;
        }
        try {
            ClassNode myNode = Act.app().classLoader().classInfoRepository().findNode(metaInfo.type().getClassName());
            return myNode.hasInterface(Map.class.getName()) || myNode.hasInterface(AdaptiveMap.class.getName());
        } catch (NullPointerException e) {
            // ignore the error in unit test
            return false;
        }
    }

    static MethodVisitor equalsMethodBegin(ClassVisitor cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
        mv.visitCode();
        return mv;
    }
    static void equalsMethodEnd(MethodVisitor mv) {
        mv.visitMaxs(0, 0); // just pass any number and have ASM to calculate
        mv.visitEnd();
    }
    static MethodVisitor hashCodeMethodBegin(ClassVisitor cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        mv.visitCode();
        return mv;
    }
    static void hashCodeMethodEnd(MethodVisitor mv, int fieldCnt) {
        FastStr signature;
        if (fieldCnt < 6) {
            signature = FastStr.of("Ljava/lang/Object;").times(fieldCnt);
        } else {
            signature = FastStr.of("Ljava/lang/Object;").times(5).append("[Ljava/lang/Object;");
        }
        signature = signature.prepend("(").append(")I");
        mv.visitMethodInsn(INVOKESTATIC, "org/osgl/Osgl", "hc", signature.toString(), false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0); // just pass any number and have ASM to calculate
        mv.visitEnd();
    }

}
