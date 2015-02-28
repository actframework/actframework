package playground;

import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.App;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.asm.ClassReader;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.asm.util.TraceClassVisitor;
import org.osgl.oms.controller.ControllerClassEnhancer;
import org.osgl.util.IO;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by luog on 18/01/2015.
 */
public class Main extends ClassLoader {
    @Override
    protected synchronized Class<?> loadClass(final String name,
            final boolean resolve) throws ClassNotFoundException {
        if (!name.equals("playground.C1")) {
            return super.loadClass(name, resolve);
        }

        System.out.println("Adapt: loading class '" + name
                + "' with on the fly adaptation");
        // gets an input stream to read the bytecode of the class
        String resource = name.replace('.', '/') + ".class";
        InputStream is = getResourceAsStream(resource);
        byte[] b;

        // adapts the class on the fly
        try {
            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(0);
            ClassVisitor cv = new ControllerClassEnhancer(cw);
            cr.accept(cv, 0);
            b = cw.toByteArray();
            OutputStream os1 = new FileOutputStream("t:\\tmp\\4.class");
            IO.write(b, os1);
            System.out.println("------------ TRANSFORMED -----------");
            cr = new ClassReader(b);
            cw = new ClassWriter(0);
            OutputStream os2 = new FileOutputStream("t:\\tmp\\4.java");
            ClassVisitor tv = new TraceClassVisitor(cw, new PrintWriter(os2));
            cr.accept(tv, 0);
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }

        // returns the adapted class
        return defineClass(name, b, 0, b.length);
    }

    public static void main(final String args[]) throws Throwable {
        // loads the application class (in args[0]) with an Adapt class loader
        ClassLoader loader = new Main();
        String s = args.length == 0 ? "playground.C1" : args[0];
        Class<C1> c = (Class<C1>)loader.loadClass(s);
        Method m = c.getMethod("doIt", String.class, AppContext.class, String.class, boolean.class);
        AppConfig cfg = mock(AppConfig.class);
        App app = mock(App.class);
        H.Request req = mock(H.Request.class);
        H.Response resp = mock(H.Response.class);
        AppContext ctx = AppContext.create(app, req, resp);
        ctx.saveLocal();
        try {
            m.invoke(c.newInstance(), "id_0", ctx, "green@osgl.org", false);
            System.out.println("Render failed");
        } catch (InvocationTargetException e) {
            System.out.println(e);
            Throwable e0 = e.getTargetException();
            if (e0 instanceof Result) {
                System.out.println("Result rendered: ");
                System.out.println(e0);
                assertEquals("green@osgl.org", ctx.renderArg("email"));
                assertEquals("id_0", ctx.renderArg("id"));
                assertEquals(0, ctx.renderArg("i"));
                assertEquals(1, ctx.renderArg("j"));
                assertSame(false, ctx.renderArg("b"));
            } else {
                e0.printStackTrace();
                //throw e0;
            }
        }
        //App.router().debug(System.out);
    }
}
