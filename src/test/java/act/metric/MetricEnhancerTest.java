package act.metric;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.app.TestingAppClassLoader;
import act.asm.ClassReader;
import act.asm.ClassVisitor;
import act.asm.ClassWriter;
import act.asm.util.TraceClassVisitor;
import act.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.$;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.*;
import java.util.List;

import static org.mockito.Mockito.when;

public class MetricEnhancerTest extends ActTestBase {

    MetricMetaInfoRepo repo;
    private File base;
    private TestingAppClassLoader classLoader;

    @Before
    public void setup() throws Exception {
        super.setup();
        repo = Mockito.mock(MetricMetaInfoRepo.class);
        base = new File("./target/test-classes");
        when(mockApp.classLoader()).thenReturn(classLoader);
        classLoader = new TestingAppClassLoader(mockApp);
    }

    @Test
    public void test() throws Exception {
        //scan();
        Class<?> c = new TestAppClassLoader().loadClass("testapp.metric.TestBed");
        Object o = $.newInstance(c);
    }

    private void scan() {
        List<File> files = Files.filter(base, _F.SAFE_CLASS);
        for (File file : files) {
            classLoader.preloadClassFile(base, file);
        }
        //File file = new File(base, ClassNames.classNameToClassFileName(className));
        //classLoader.preloadClassFile(base, file);
        classLoader.scan();
    }


    private class TestAppClassLoader extends ClassLoader {
        @Override
        protected synchronized Class<?> loadClass(final String name,
                                                  final boolean resolve) throws ClassNotFoundException {
            if (!name.equals("testapp.metric.TestBed")) {
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
                MetricEnhancer enhancer = new MetricEnhancer(repo, cw);
                cr.accept(enhancer, ClassReader.EXPAND_FRAMES);
                b = cw.toByteArray();
                OutputStream os1 = new FileOutputStream("/tmp/" + S.afterLast(cn, "/") + ".class");
                IO.write(b, os1);
                cr = new ClassReader(b);
                cw = new ClassWriter(0);
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

    private enum _F {
        ;
        static $.Predicate<String> SYS_CLASS_NAME = new $.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("java") || s.startsWith("org.osgl.");
            }
        };
        static $.Predicate<String> SAFE_CLASS = S.F.endsWith(".class").and(SYS_CLASS_NAME.negate());
    }
}
