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

import act.ActTestBase;
import act.asm.ClassReader;
import act.asm.ClassVisitor;
import act.asm.ClassWriter;
import act.asm.util.TraceClassVisitor;
import org.junit.Before;
import org.junit.Test;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

public class DataObjectEnhancerTest extends ActTestBase {

    public static final String TMPL_PATH = "/path/to/template";

    TestAppClassLoader classLoader;
    Object addr1;
    Object addr2;
    Object person1;
    Object person2;
    Object student1;
    Object student2;
    Object teacher1;
    Object teacher2;
    Object male;
    Object female;
    private String streetNo = "5";
    private String streetName = "George St";
    private String city = "Sydney";
    private String firstName = "Tom";
    private String lastName = "Goodman";
    private Integer age = 22;
    private String clazz = "Class A";
    private String studentId = "xyy";
    private Double score = 99.2d;
    private String teacherId = "123";
    private Method happyBirthday;

    @Before
    public void setup() throws Exception {
        super.setup();
        classLoader = new TestAppClassLoader();
        Class<? extends Enum> genderCls = $.cast(load("Person2$Gender"));
        male = Enum.valueOf(genderCls, "M");
        female = Enum.valueOf(genderCls, "F");
        Class<?> addrCls = load("Address2");
        addr1 = $.newInstance(addrCls, streetNo, streetName, city);
        addr2 = $.newInstance(addrCls, streetNo, streetName, city);
        Class<?> personCls = load("Person2");
        person1 = $.newInstance(personCls, firstName, lastName, addr1, age, male);
        person2 = $.newInstance(personCls, firstName, lastName, addr2, age, male);
        Class<?> studentCls = load("Student2");
        student1 = $.newInstance(studentCls, firstName, lastName, addr1, age, female, clazz, studentId, score);
        student2 = $.newInstance(studentCls, firstName, lastName, addr1, age, female, clazz, studentId, score);
        Class<?> teacherCls = load("Teacher2");
        teacher1 = $.newInstance(teacherCls, firstName, lastName, addr1, age, female, teacherId);
        teacher2 = $.newInstance(teacherCls, firstName, lastName, addr1, age, female, teacherId);
        happyBirthday = personCls.getDeclaredMethod("happyBirthday");
    }

    @Test
    public void addressEqualTest() {
        eq(addr1, addr2);
    }

    @Test
    public void addressHashCodeTest() {
        eq(addr1.hashCode(), addr2.hashCode());
    }

    @Test
    public void personEqualTest() {
        eq(person1, person2);
    }

    @Test
    public void personHashCodeTest() {
        eq(person1.hashCode(), person2.hashCode());
    }

    @Test
    public void studentEqualTest() {
        eq(student1, student2);
        happyBirthday(student1);
        ne(student1, student2);
    }

    @Test
    public void studentHashCodeTest() {
        eq(student1.hashCode(), student2.hashCode());
        happyBirthday(student1);
        ne(student1.hashCode(), student2.hashCode());
    }

    @Test
    public void teacherEqualTest() {
        eq(teacher1, teacher2);
        happyBirthday(teacher1);
        eq(teacher1, teacher2);
    }

    @Test
    public void teacherHashCodeTest() {
        eq(teacher1.hashCode(), teacher2.hashCode());
        happyBirthday(teacher1);
        eq(teacher1.hashCode(), teacher2.hashCode());
    }

    private void happyBirthday(Object o) {
        try {
            happyBirthday.invoke(o);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    private Class<?> load(String className) throws Exception {
        String cn = "testapp.model." + className;
        return classLoader.loadClass(cn);
    }

    private class TestAppClassLoader extends ClassLoader {
        @Override
        protected synchronized Class<?> loadClass(final String name,
                                                  final boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("testapp.")) {
                return super.loadClass(name, resolve);
            }

            // gets an input stream to read the bytecode of the class
            String cn = name.replace('.', '/');
            String resource = cn + ".class";
            InputStream is = getResourceAsStream(resource);
            byte[] b;

            // adapts the class on the fly
            try {
                ClassReader cr = new ClassReader(is);
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                DataObjectEnhancer enhancer = new DataObjectEnhancer(cw);
                cr.accept(enhancer, 0);
                b = cw.toByteArray();
                //CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), true, new PrintWriter(System.out));
                OutputStream os1 = new FileOutputStream("/tmp/" + S.afterLast(cn, "/") + ".class");
                IO.write(b, os1);
                cr = new ClassReader(b);
                cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                OutputStream os2 = new FileOutputStream("/tmp/" + S.afterLast(cn, "/") + ".java");
                ClassVisitor tv = new TraceClassVisitor(cw, new PrintWriter(os2));
                cr.accept(tv, 0);
            } catch (Exception e) {
                throw new ClassNotFoundException(name, e);
            }

            // returns the adapted class
            return defineClass(name, b, 0, b.length);
        }

    }

}
