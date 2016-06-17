package act.conf;

import act.TestBase;
import act.app.conf.AutoConfig;
import act.app.conf.AutoConfigPlugin;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.Const;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

public class AutoConfigPluginTest extends TestBase {

    private int intVal = 37;
    private long longVal = 21345315436L;
    private boolean boolVal = true;
    private String stringVal = $.randomStr();
    private H.Method enumVal = H.Method.CONNECT;

    @Before
    public void before() throws Exception {
        setup();
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".int")))).thenReturn(intVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".long")))).thenReturn(longVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".bool")))).thenReturn(boolVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".str")))).thenReturn(stringVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".method")))).thenReturn(enumVal);
    }

    @Test
    public void testProvisionConfigData() {
        AutoConfigPlugin.loadPluginAutoConfig(Foo.class, mockApp);
        eq(intVal, Foo.foo_int);
        eq(intVal, Foo.foo_int_val.get());

        eq(longVal, Foo.foo_long);
        eq(longVal, Foo.foo_long_val.get());

        eq(boolVal, Foo.foo_bool);
        eq(boolVal, Foo.foo_bool_val.get());

        eq(stringVal, Foo.foo_str);
        eq(stringVal, Foo.FOO_STR_VAL.get());

        eq(enumVal, Foo.foo_method);
        eq(enumVal, Foo.FOO_METHOD_VAL.get());
    }

    @AutoConfig
    private static class Foo {

        static int foo_int;
        static final Const<Integer> foo_int_val = $.constant(0);

        static long foo_long;
        static final Const<Long> foo_long_val = $.constant(0L);

        static boolean foo_bool;
        static final Const<Boolean> foo_bool_val = $.constant(false);

        static String foo_str;
        static final Const<String> FOO_STR_VAL = $.constant("UNIX");

        static H.Method foo_method;
        static final $.Val<H.Method> FOO_METHOD_VAL = $.val(H.Method.GET);
    }

    private class ContainsIgnoreCase extends ArgumentMatcher<String> {

        private final String substring;

        ContainsIgnoreCase(String substring) {
            this.substring = substring;
        }

        public boolean matches(Object actual) {
            return actual != null && ((String) actual).toUpperCase().contains(substring.toUpperCase());
        }

        public void describeTo(Description description) {
            description.appendText("containsIgnoreCase(\"" + substring + "\")");
        }

    }

}
