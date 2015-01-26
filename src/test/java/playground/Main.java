package playground;

import junit.framework.Assert;
import org.mockito.Mockito;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.server.App;
import org.osgl.mvc.server.AppConfig;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.asm.ClassReader;
import org.osgl.mvc.server.asm.ClassVisitor;
import org.osgl.mvc.server.asm.ClassWriter;
import org.osgl.mvc.server.asm.util.TraceClassVisitor;

import java.io.InputStream;
import java.io.PrintWriter;
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
        if (name.startsWith("java.") || name.startsWith("org.osgl.")) {
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
            ClassVisitor cv = new ControllerClassVisitor(cw);
            ClassVisitor tv = new TraceClassVisitor(cv, new PrintWriter(System.out));
            cr.accept(cv, 0);
            b = cw.toByteArray();
            System.out.println("------------ TRANSFORMED -----------");
            cr = new ClassReader(b);
            cw = new ClassWriter(0);
            tv = new TraceClassVisitor(cw, new PrintWriter(System.out));
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
        Method m = c.getMethod("doIt", String.class, String.class, AppContext.class);
        AppConfig cfg = mock(AppConfig.class);
        H.Request req = mock(H.Request.class);
        H.Response resp = mock(H.Response.class);
        AppContext.init(cfg, req, resp);
        AppContext ctx = AppContext.get();
        try {
            m.invoke(c.newInstance(), "id_0", "green@osgl.org", ctx);
            System.out.println("Render failed");
        } catch (Result r) {
            System.out.println("Result rendered: ");
            System.out.println(r);
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
