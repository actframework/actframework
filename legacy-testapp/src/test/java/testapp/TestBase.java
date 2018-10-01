package testapp;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runner.JUnitCore;
import org.osgl.util.S;

@Ignore
public class TestBase extends Assert {

    protected void same(Object a, Object b) {
        assertSame(a, b);
    }

    protected void eq(Object[] a1, Object[] a2) {
        assertArrayEquals(a1, a2);
    }

    protected void eq(Object o1, Object o2) {
        assertEquals(o1, o2);
    }

    protected void ceq(Object o1, Object o2) {
        assertEquals(S.string(o1), S.string(o2));
    }

    protected void yes(Boolean expr, String msg, Object... args) {
        assertTrue(S.fmt(msg, args), expr);
    }

    protected void yes(Boolean expr) {
        assertTrue(expr);
    }

    protected void no(Boolean expr, String msg, Object... args) {
        assertFalse(S.fmt(msg, args), expr);
    }

    protected void no(Boolean expr) {
        assertFalse(expr);
    }

    protected void fail(String msg, Object... args) {
        assertFalse(S.fmt(msg, args), true);
    }

    protected static void run(Class<? extends TestBase> cls) {
        new JUnitCore().run(cls);
    }

    protected static void println(String tmpl, Object... args) {
        System.out.println(String.format(tmpl, args));
    }


}
