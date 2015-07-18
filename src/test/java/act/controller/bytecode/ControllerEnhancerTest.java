package act.controller.bytecode;

import act.app.AppByteCodeScanner;
import act.app.AppCodeScannerManager;
import act.app.TestingAppClassLoader;
import act.asm.ClassVisitor;
import act.asm.util.TraceClassVisitor;
import act.controller.meta.ControllerClassMetaInfoHolder;
import act.controller.meta.ControllerClassMetaInfoManager;
import act.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.osgl._;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import act.TestBase;
import act.app.ActionContext;
import act.asm.ClassReader;
import act.asm.ClassWriter;
import act.controller.meta.ControllerClassMetaInfo;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;
import testapp.util.InvokeLog;
import testapp.util.InvokeLogFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControllerEnhancerTest extends TestBase implements ControllerClassMetaInfoHolder {

    public static final String TMPL_PATH = "/path/to/template";

    protected String cn;
    protected Class<?> cc;
    protected Object c;
    protected Method m;
    protected InvokeLog invokeLog;
    protected ActionContext ctx;
    private TestingAppClassLoader classLoader;
    private AppCodeScannerManager scannerManager;
    private AppByteCodeScanner scanner;
    protected ControllerClassMetaInfoManager infoSrc;
    private File base;

    @Override
    public ControllerClassMetaInfo controllerClassMetaInfo(String className) {
        return infoSrc.controllerMetaInfo(className);
    }

    @Before
    public void setup() throws Exception {
        super.setup();
        invokeLog = mock(InvokeLog.class);
        scanner = new ControllerByteCodeScanner();
        scanner.setApp(mockApp);
        classLoader = new TestingAppClassLoader(mockApp);
        infoSrc = classLoader.controllerClassMetaInfoManager();
        scannerManager = mock(AppCodeScannerManager.class);
        when(mockApp.classLoader()).thenReturn(classLoader);
        when(mockApp.scannerManager()).thenReturn(scannerManager);
        when(mockAppConfig.possibleControllerClass(anyString())).thenReturn(true);
        when(mockRouter.isActionMethod(anyString(), anyString())).thenReturn(false);
        C.List<AppByteCodeScanner> scanners = C.list(scanner);
        when(scannerManager.byteCodeScanners()).thenReturn(scanners);
        InvokeLogFactory.set(invokeLog);
        ActionContext.clearCurrent();
        ctx = ActionContext.create(mockApp, mockReq, mockResp);
        base = new File("./target/test-classes");
    }

    @Test
    public void returnOk() throws Exception {
        prepare("ReturnOk");
        m = method();
        Object r = m.invoke(c);
        eq(r, Ok.INSTANCE);
    }

    @Test
    public void staticReturnOk() throws Exception {
        prepare("StaticReturnOk");
        m = method();
        Object r = m.invoke(null);
        eq(r, Ok.INSTANCE);
    }

    @Test
    public void throwOk() throws Exception {
        prepare("ThrowOk");
        m = method();
        try {
            m.invoke(c);
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Ok) {
                // success
                return;
            }
            throw e;
        }
    }

    @Test
    public void staticThrowOk() throws Exception {
        prepare("StaticThrowOk");
        m = method();
        try {
            m.invoke(null);
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Ok) {
                // success
                return;
            }
            throw e;
        }
    }

    @Test
    public void voidOk() throws Throwable {
        prepare("VoidOk");
        m = method();
        try {
            m.invoke(c);
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Ok) {
                // success
                return;
            }
            throw e.getCause();
        }
    }

    @Test
    public void voidOkWithNotFound() throws Throwable {
        prepare("VoidOkWithNotFound");
        m = method(boolean.class);
        try {
            m.invoke(c, true);
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof NotFound) {
                // success
                return;
            }
            throw e.getCause();
        }
    }

    @Test
    public void returnResultWithParamAppCtxLocal() throws Exception {
        prepare("ReturnResultWithParam");
        m = method(int.class, String.class);
        //ctx.saveLocal();
        Object r = m.invoke(c, 100, "foo");
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
    }

    @Test
    public void returnResultWithParamAndTemplatePath() throws Exception {
        prepare("ReturnResultWithParamAndTemplatePath");
        m = method(int.class, String.class);
        //ctx.saveLocal();
        Object r = m.invoke(c, 100, "foo");
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
        eq(TMPL_PATH, ctx.templatePath());
    }

    @Test
    public void staticReturnResultWithParamAppCtxLocal() throws Exception {
        prepare("StaticReturnResultWithParam");
        m = method(int.class, String.class);
        //ctx.saveLocal();
        Object r = m.invoke(null, 100, "foo");
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
    }

    @Test
    public void throwResultWithParamAppCtxLocal() throws Exception {
        prepare("ThrowResultWithParam");
        m = method(int.class, String.class);
        //ctx.saveLocal();
        try {
            m.invoke(c, 100, "foo");
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Result) {
                // success
                eq(100, ctx.renderArg("foo"));
                eq("foo", ctx.renderArg("bar"));
                return;
            }
            throw e;
        }
    }

    @Test
    public void staticThrowResultWithParamAppCtxLocal() throws Exception {
        prepare("StaticThrowResultWithParam");
        m = method(int.class, String.class);
        //ctx.saveLocal();
        try {
            m.invoke(c, 100, "foo");
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Result) {
                // success
                eq(100, ctx.renderArg("foo"));
                eq("foo", ctx.renderArg("bar"));
                return;
            }
            throw e;
        }
    }

    @Test
    public void voidResultWithParamAppCtxLocal() throws Exception {
        prepare("VoidResultWithParam");
        m = method(int.class, String.class);
        //ctx.saveLocal();
        try {
            m.invoke(c, 100, "foo");
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Result) {
                // success
                eq(100, ctx.renderArg("foo"));
                eq("foo", ctx.renderArg("bar"));
                return;
            }
            throw e;
        }
    }

    @Test
    public void staticVoidResultWithParamAppCtxLocal() throws Exception {
        prepare("StaticVoidResultWithParam");
        m = method(int.class, String.class);
        //ctx.saveLocal();
        try {
            m.invoke(null, 100, "foo");
            fail("Result expected to be thrown out");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Result) {
                // success
                eq(100, ctx.renderArg("foo"));
                eq("foo", ctx.renderArg("bar"));
                return;
            }
            throw e;
        }
    }

    @Test
    public void returnResultWithParamAppCtxParam() throws Exception {
        prepare("ReturnResultWithParamCtxParam");
        m = method(int.class, String.class, ActionContext.class);
        Object r = m.invoke(c, 100, "foo", ctx);
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
    }

    @Test
    public void returnResultWithParamAppCtxField() throws Exception {
        prepare("ReturnResultWithParamCtxField");
        m = method(int.class, String.class);
        Method setCtx = cc.getMethod("setAppContext", ActionContext.class);
        setCtx.invoke(c, ctx);
        Object r = m.invoke(c, 100, "foo");
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
    }

    /**
     * GH issue #2
     * @throws Exception
     */
    @Test
    public void voidResultWithParamAppCtxFieldAndEmptyBody() throws Exception {
        prepare("VoidResultWithParamCtxFieldEmptyBody");
        m = method(String.class, int.class);
        //ctx.saveLocal();
        try {
            m.invoke(c, "foo", 100);
            fail("It shall throw out a Result");
        } catch (InvocationTargetException e) {
            Throwable r = e.getCause();
            yes(r instanceof Result, "r shall be of type Result, found: %s", E.stackTrace(r));
            eq(100, ctx.renderArg("age"));
            eq("foo", ctx.renderArg("who"));
        }
    }

    private void prepare(String className) throws Exception {
        cn = "testapp.controller." + className;
        scan(cn);
        cc = new TestAppClassLoader().loadClass(cn);
        c = _.newInstance(cc);
    }

    private Method method(Class... types) throws Exception {
        return cc.getDeclaredMethod("handle", types);
    }

    private Field field(String name) throws Exception {
        Field f = cc.getField(name);
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
        infoSrc.mergeActionMetaInfo();
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
                ControllerEnhancer enhancer = new ControllerEnhancer(cw, ControllerEnhancerTest.this);
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

    private static enum _F {
        ;
        static _.Predicate<String> SYS_CLASS_NAME = new _.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("java") || s.startsWith("org.osgl.");
            }
        };
        static _.Predicate<String> SAFE_CLASS = S.F.endsWith(".class").and(SYS_CLASS_NAME.negate());
    }
}
