package act.mail;

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
import act.app.AppByteCodeScanner;
import act.app.AppCodeScannerManager;
import act.app.TestingAppClassLoader;
import act.asm.ClassReader;
import act.asm.ClassVisitor;
import act.asm.ClassWriter;
import act.asm.util.TraceClassVisitor;
import act.mail.bytecode.MailerByteCodeScanner;
import act.mail.bytecode.MailerEnhancer;
import act.mail.meta.MailerClassMetaInfo;
import act.mail.meta.MailerClassMetaInfoHolder;
import act.mail.meta.MailerClassMetaInfoManager;
import act.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.S;
import testapp.util.InvokeLog;
import testapp.util.InvokeLogFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MailerEnhancerTest extends ActTestBase implements MailerClassMetaInfoHolder {

    public static final String TMPL_PATH = "/path/to/template";

    protected String mailerName;
    protected Class<?> mailerClass;
    protected Object mailer;
    protected Method m;
    protected InvokeLog invokeLog;
    private TestingAppClassLoader classLoader;
    private AppCodeScannerManager scannerManager;
    private AppByteCodeScanner scanner;
    protected MailerClassMetaInfoManager infoSrc;
    private File base;

    @Override
    public MailerClassMetaInfo mailerClassMetaInfo(String className) {
        return infoSrc.mailerMetaInfo(className);
    }

    @Before
    public void setup() throws Exception {
        super.setup();
        invokeLog = mock(InvokeLog.class);
        scanner = new MailerByteCodeScanner();
        scanner.setApp(mockApp);
        classLoader = new TestingAppClassLoader(mockApp);
        infoSrc = classLoader.mailerClassMetaInfoManager();
        scannerManager = mock(AppCodeScannerManager.class);
        when(mockApp.classLoader()).thenReturn(classLoader);
        when(mockApp.scannerManager()).thenReturn(scannerManager);
        C.List<AppByteCodeScanner> scanners = C.list(scanner);
        when(scannerManager.byteCodeScanners()).thenReturn(scanners);
        InvokeLogFactory.set(invokeLog);
        base = new File("./target/test-classes");
    }

    @Test
    public void sendX() throws Exception {
        ArgumentCaptor<MailerContext> captor = ArgumentCaptor.forClass(MailerContext.class);
        //TODO verify MailerContext operations during sendX call
//        PowerMockito.mockStatic(Mailer.Util.class);
//        PowerMockito.verifyStatic();
        prepare("MyMailer");
        m = method("sendX", String.class, String.class);
        m.invoke(mailer, "username", "password");
    }

    @Test
    public void sendY() throws Exception {
        ArgumentCaptor<MailerContext> captor = ArgumentCaptor.forClass(MailerContext.class);
        //TODO verify MailerContext operations during sendX call
//        PowerMockito.mockStatic(Mailer.Util.class);
//        PowerMockito.verifyStatic();
        prepare("MyMailer");
        m = method("sendY", int.class, long.class);
        m.invoke(mailer, 1, 5l);
        // todo verify the jobManager.now() return
    }

    private void prepare(String className) throws Exception {
        mailerName = "testapp.mail." + className;
        scan(mailerName);
        mailerClass = new TestAppClassLoader().loadClass(mailerName);
        mailer = $.newInstance(mailerClass);
    }

    private Method method(String name, Class... types) throws Exception {
        return mailerClass.getDeclaredMethod(name, types);
    }

    private Field field(String name) throws Exception {
        Field f = mailerClass.getField(name);
        f.setAccessible(true);
        return f;
    }

    private void scan(String className) {
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
                ClassWriter cw = new ClassWriter(0);
                MailerEnhancer enhancer = new MailerEnhancer(cw, MailerEnhancerTest.this);
                cr.accept(enhancer, 0);
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
