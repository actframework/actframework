package org.osgl.oms.controller.bytecode;

import org.junit.Before;
import org.junit.Test;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import org.osgl.oms.TestBase;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.asm.ClassReader;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.asm.util.TraceClassVisitor;
import org.osgl.oms.controller.meta.ControllerClassMetaInfo;
import org.osgl.oms.controller.meta.ControllerClassMetaInfoHolder;
import org.osgl.oms.controller.meta.ControllerClassMetaInfoManager;
import org.osgl.util.IO;
import org.osgl.util.S;
import testapp.util.InvokeLog;
import testapp.util.InvokeLogFactory;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;

public class ControllerEnhancerTest extends TestBase implements ControllerClassMetaInfoHolder {

    protected String cn;
    protected Class<?> cc;
    protected Object c;
    protected Method m;
    protected Field f;
    protected InvokeLog invokeLog;
    protected AppContext ctx;
    protected ControllerClassMetaInfoManager infoSrc;

    @Override
    public ControllerClassMetaInfo controllerClassMetaInfo(String className) {
        return infoSrc.controllerMetaInfo(className);
    }

    @Before
    public void setup() throws Exception {
        super.setup();
        infoSrc = new ControllerClassMetaInfoManager(new _.Factory<ControllerScanner>(){
            @Override
            public ControllerScanner create() {
                return new ControllerScanner(mockAppConfig, mockRouter, new _.F1<String, byte[]>() {
                    @Override
                    public byte[] apply(String s) throws NotAppliedException, _.Break {
                        return loadBytecode(s);
                    }
                });
            }
        });
        invokeLog = mock(InvokeLog.class);
        InvokeLogFactory.set(invokeLog);
        AppContext.clear();
        ctx = AppContext.create(mockApp, mockReq, mockResp);
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
    public void voidOk() throws Exception {
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
            throw e;
        }
    }

    @Test
    public void returnResultWithParamAppCtxLocal() throws Exception {
        prepare("ReturnResultWithParam");
        m = method(int.class, String.class);
        ctx.saveLocal();
        Object r = m.invoke(c, 100, "foo");
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
    }

    @Test
    public void staticReturnResultWithParamAppCtxLocal() throws Exception {
        prepare("StaticReturnResultWithParam");
        m = method(int.class, String.class);
        ctx.saveLocal();
        Object r = m.invoke(null, 100, "foo");
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
    }

    @Test
    public void throwResultWithParamAppCtxLocal() throws Exception {
        prepare("ThrowResultWithParam");
        m = method(int.class, String.class);
        ctx.saveLocal();
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
        ctx.saveLocal();
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
        ctx.saveLocal();
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
        ctx.saveLocal();
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
        m = method(int.class, String.class, AppContext.class);
        Object r = m.invoke(c, 100, "foo", ctx);
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
    }

    @Test
    public void returnResultWithParamAppCtxField() throws Exception {
        prepare("ReturnResultWithParamCtxField");
        m = method(int.class, String.class);
        Method setCtx = cc.getMethod("setAppContext", AppContext.class);
        setCtx.invoke(c, ctx);
        Object r = m.invoke(c, 100, "foo");
        yes(r instanceof Result);
        eq(100, ctx.renderArg("foo"));
        eq("foo", ctx.renderArg("bar"));
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
        infoSrc.scanForControllerMetaInfo(className);
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
}
