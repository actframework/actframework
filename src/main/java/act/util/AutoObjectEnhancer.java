package act.util;

import act.ActComponent;
import act.asm.*;
import act.data.Data;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.FastStr;

/**
 * A tool to enhance a object by generating common {@link Object}
 * methods, e.g. {@link Object#equals(Object)}
 */
@ActComponent
public class AutoObjectEnhancer extends AppByteCodeEnhancer<AutoObjectEnhancer> {

    private ObjectMetaInfo metaInfo;

    public AutoObjectEnhancer() {
        super($.F.<String>yes());
    }

    protected AutoObjectEnhancer(ClassVisitor cv) {
        super($.F.<String>yes(), cv);
    }

    @Override
    protected Class<AutoObjectEnhancer> subClass() {
        return AutoObjectEnhancer.class;
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
        if (metaInfo.hasAutoObjectAnnotation()) {
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

                @Override
                public void visitAttribute(Attribute attr) {
                    System.out.println("Found attribute: " + attr);
                    super.visitAttribute(attr);
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
                }
            };
        }
        return def;
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
        if (metaInfo.shouldCallSuper()) {
            mv.visitVarInsn(ALOAD, 0); // load this
            mv.visitVarInsn(ALOAD, 2); // load that
            mv.visitMethodInsn(INVOKESPECIAL, metaInfo.superType().getInternalName(), "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFEQ, lbl_exit_with_false);
        }
        // call Osgl.eq(that.f1, this.f1)
        C.List<ObjectMetaInfo.FieldMetaInfo> fields = metaInfo.fields();
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

    private void generateHashCodeMethod() {
        MethodVisitor mv = hashCodeMethodBegin(this);
        Type host = metaInfo.type();
        C.List<ObjectMetaInfo.FieldMetaInfo> fields = metaInfo.fields();
        int cnt = 0;
        for (ObjectMetaInfo.FieldMetaInfo fi : fields) {
            boolean added = fi.addHashCodeInstruction(host, mv);
            if (added) {
                cnt++;
            }
        }
        if (metaInfo.shouldCallSuper()) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, metaInfo.superType().getInternalName(), "hashCode", "()I", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            cnt++;
        }
        hashCodeMethodEnd(mv, cnt);
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
