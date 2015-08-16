package act;

import act.conf.AppConfig;
import act.job.AppJobManager;
import act.route.Router;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runner.JUnitCore;
import org.mockito.Matchers;
import org.mockito.internal.matchers.StartsWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl._;
import org.osgl.http.H;
import act.app.App;
import act.app.ActionContext;
import act.util.ClassNames;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    protected static <T> T fieldVal(Object entity, String field) {
        try {
            Field f = entity.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return (T) f.get(entity);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public static File root() {
        FastStr fs = FastStr.of(TestBase.class.getClassLoader().getResource("routes").getPath());
        FastStr classRoot = fs.beforeLast("/");
        FastStr target = classRoot.beforeLast("/");
        return new File(target.toString());
    }

    protected Router mockRouter;
    protected ActionContext mockActionContext;
    protected AppConfig mockAppConfig;
    protected AppJobManager mockJobManager;
    protected App mockApp;
    protected H.Request mockReq;
    protected H.Response mockResp;

    protected void setup() throws Exception {
        mockApp = mock(App.class);
        Field f = App.class.getDeclaredField("INST");
        f.setAccessible(true);
        f.set(null, mockApp);
        mockJobManager = mock(AppJobManager.class);
        when(mockApp.jobManager()).thenReturn(mockJobManager);
        mockAppConfig = mock(AppConfig.class);
        when(mockAppConfig.possibleControllerClass(argThat(new StartsWith("testapp.controller.")))).thenReturn(true);
        mockActionContext = mock(ActionContext.class);
        when(mockActionContext.app()).thenReturn(mockApp);
        when(mockActionContext.config()).thenReturn(mockAppConfig);
        when(mockActionContext.newInstance(any(Class.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return _.newInstance((Class) args[0]);
            }
        });
        mockRouter = mock(Router.class);
        when(mockApp.config()).thenReturn(mockAppConfig);
        when(mockApp.router()).thenReturn(mockRouter);
        when(mockApp.router(Matchers.same(""))).thenReturn(mockRouter);
        when(mockApp.newInstance(any(Class.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return _.newInstance((Class)args[0]);
            }
        });
        mockReq = mock(H.Request.class);
        mockResp = mock(H.Response.class);
    }

    protected byte[] loadBytecode(String className) {
        String fileName = ClassNames.classNameToClassFileName(className);
        InputStream is = getClass().getResourceAsStream(fileName);
        return IO.readContent(is);
    }

}