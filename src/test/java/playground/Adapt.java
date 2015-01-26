package playground;

import org.osgl.mvc.server.asm.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * @author Eric Bruneton
 */
public class Adapt extends ClassLoader {

    @Override
    protected synchronized Class<?> loadClass(final String name,
            final boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("java.") || name.startsWith("org.osgl.")) {
            System.err.println("Adapt: loading class '" + name
                    + "' without on the fly adaptation");
            return super.loadClass(name, resolve);
        } else {
            System.err.println("Adapt: loading class '" + name
                    + "' with on the fly adaptation");
        }

        // gets an input stream to read the bytecode of the class
        String resource = name.replace('.', '/') + ".class";
        InputStream is = getResourceAsStream(resource);
        byte[] b;

        // adapts the class on the fly
        try {
            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(0);
            ClassVisitor cv = new TraceFieldClassAdapter(cw);
            cr.accept(cv, 0);
            b = cw.toByteArray();
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }

        // optional: stores the adapted class on disk
        try {
            FileOutputStream fos = new FileOutputStream(resource + ".adapted");
            fos.write(b);
            fos.close();
        } catch (IOException e) {
        }

        // returns the adapted class
        return defineClass(name, b, 0, b.length);
    }

    public static void main(final String args[]) throws Exception {
        // loads the application class (in args[0]) with an Adapt class loader
        ClassLoader loader = new Adapt();
        String s = args.length == 0 ? "playground.Adaptee" : args[0];
        Class<?> c = loader.loadClass(s);
        // calls the 'main' static method of this class with the
        // application arguments (in args[1] ... args[n]) as parameter
        Method m = c.getMethod("main", new Class<?>[] { String[].class });
        String[] applicationArgs = args.length > 0 ? new String[args.length - 1] : new String[0];
        if (args.length > 0) {
            System.arraycopy(args, 1, applicationArgs, 0, applicationArgs.length);
        }
        m.invoke(null, new Object[] { applicationArgs });
    }
}

class TraceFieldClassAdapter extends ClassVisitor implements Opcodes {

    private String owner;

    public TraceFieldClassAdapter(final ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(final int version, final int access, final String name,
            final String signature, final String superName,
            final String[] interfaces) {
        owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name,
            final String desc, final String signature, final Object value) {
        FieldVisitor fv = super
                .visitField(access, name, desc, signature, value);
        if ((access & ACC_STATIC) == 0) {
            Type t = Type.getType(desc);
            int size = t.getSize();

            // generates getter method
            String gDesc = "()" + desc;
            MethodVisitor gv = cv.visitMethod(ACC_PRIVATE, "_get" + name,
                    gDesc, null, null);
            gv.visitFieldInsn(GETSTATIC, "java/lang/System", "err",
                    "Ljava/io/PrintStream;");
            gv.visitLdcInsn("_get" + name + " called");
            gv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
            gv.visitVarInsn(ALOAD, 0);
            gv.visitFieldInsn(GETFIELD, owner, name, desc);
            gv.visitInsn(t.getOpcode(IRETURN));
            gv.visitMaxs(1 + size, 1);
            gv.visitEnd();
            System.err.printf("getter for %s generated\n", name);

            // generates setter method
            String sDesc = "(" + desc + ")V";
            MethodVisitor sv = cv.visitMethod(ACC_PRIVATE, "_set" + name,
                    sDesc, null, null);
            sv.visitFieldInsn(GETSTATIC, "java/lang/System", "err",
                    "Ljava/io/PrintStream;");
            sv.visitLdcInsn("_set" + name + " called");
            sv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(Ljava/lang/String;)V", false);
            sv.visitVarInsn(ALOAD, 0);
            sv.visitVarInsn(t.getOpcode(ILOAD), 1);
            sv.visitFieldInsn(PUTFIELD, owner, name, desc);
            sv.visitInsn(RETURN);
            sv.visitMaxs(1 + size, 1 + size);
            sv.visitEnd();
            System.err.printf("setter for %s generated\n", name);
        }
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name,
            final String desc, final String signature, final String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature,
                exceptions);
        return mv == null ? null : new TraceFieldCodeAdapter(mv, owner);
    }

}

class TraceFieldCodeAdapter extends MethodVisitor implements Opcodes {

    private String owner;

    public TraceFieldCodeAdapter(final MethodVisitor mv, final String owner) {
        super(Opcodes.ASM5, mv);
        this.owner = owner;
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        if (owner.equals(this.owner)) {
            if (opcode == GETFIELD) {
                // replaces GETFIELD f by INVOKESPECIAL _getf
                String gDesc = "()" + desc;
                visitMethodInsn(INVOKESPECIAL, owner, "_get" + name, gDesc,
                        false);
                return;
            } else if (opcode == PUTFIELD) {
                // replaces PUTFIELD f by INVOKESPECIAL _setf
                String sDesc = "(" + desc + ")V";
                visitMethodInsn(INVOKESPECIAL, owner, "_set" + name, sDesc,
                        false);
                return;
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }
}
