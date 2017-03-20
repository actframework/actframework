package playground;

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

import act.asm.ClassWriter;
import act.asm.MethodVisitor;
import act.asm.Opcodes;
import act.asm.Type;
import act.asm.commons.GeneratorAdapter;
import act.asm.commons.Method;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class HelloWorld extends ClassLoader implements Opcodes {
    public static void main(final String args[]) throws Exception {
        // Generates the bytecode corresponding to the following Java class:
        //
        // public class Example {
        // public static void main (String[] args) {
        // System.out.println("Hello world!");
        // }
        // }

        // creates a ClassWriter for the Example public class,
        // which inherits from Object
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_1, ACC_PUBLIC, "Example", null, "java/lang/Object", null);

        // creates a MethodWriter for the (implicit) constructor
        MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null,
                null);
        // pushes the 'this' variable
        mw.visitVarInsn(ALOAD, 0);
        // invokes the super class constructor
        mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
                false);
        mw.visitInsn(RETURN);
        // this code uses a maximum of one stack element and one local variable
        mw.visitMaxs(1, 1);
        mw.visitEnd();

        // creates a MethodWriter for the 'main' method
        mw = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main",
                "([Ljava/lang/String;)V", null, null);
        // pushes the 'out' field (of type PrintStream) of the System class
        mw.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
                "Ljava/io/PrintStream;");
        // pushes the "Hello World!" String constant
        mw.visitLdcInsn("Hello world!");
        // invokes the 'println' method (defined in the PrintStream class)
        mw.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false);
        mw.visitInsn(RETURN);
        // this code uses a maximum of two stack elements and two local
        // variables
        mw.visitMaxs(2, 2);
        mw.visitEnd();

        // gets the bytecode of the Example class, and loads it dynamically
        byte[] code = cw.toByteArray();

        FileOutputStream fos = new FileOutputStream("Example.class");
        fos.write(code);
        fos.close();

        HelloWorld loader = new HelloWorld();
        Class<?> exampleClass = loader.defineClass("Example", code, 0,
                code.length);

        // uses the dynamically generated class to print 'HelloWorld'
        exampleClass.getMethods()[0].invoke(null, new Object[] { null });

        // ------------------------------------------------------------------------
        // Same example with a GeneratorAdapter (more convenient but slower)
        // ------------------------------------------------------------------------

        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_1, ACC_PUBLIC, "Example", null, "java/lang/Object", null);

        // creates a GeneratorAdapter for the (implicit) constructor
        Method m = Method.getMethod("void <init> ()");
        GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, m, null, null,
                cw);
        mg.loadThis();
        mg.invokeConstructor(Type.getType(Object.class), m);
        mg.returnValue();
        mg.endMethod();

        // creates a GeneratorAdapter for the 'main' method
        m = Method.getMethod("void main (String[])");
        mg = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, cw);
        mg.getStatic(Type.getType(System.class), "out",
                Type.getType(PrintStream.class));
        mg.push("Hello world!");
        mg.invokeVirtual(Type.getType(PrintStream.class),
                Method.getMethod("void println (String)"));
        mg.returnValue();
        mg.endMethod();

        cw.visitEnd();

        code = cw.toByteArray();
        loader = new HelloWorld();
        exampleClass = loader.defineClass("Example", code, 0, code.length);

        // uses the dynamically generated class to print 'HelloWorld'
        exampleClass.getMethods()[0].invoke(null, new Object[] { null });
    }
}
