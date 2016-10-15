package act.conf;

import act.Act;
import act.TestBase;
import act.app.conf.AutoConfig;
import act.app.conf.AutoConfigPlugin;
import act.app.data.StringValueResolverManager;
import act.inject.DependencyInjector;
import act.inject.genie.GenieInjector;
import act.plugin.GenericPluginManager;
import org.hamcrest.Description;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.inject.Genie;
import org.osgl.inject.Injector;
import org.osgl.util.C;
import org.osgl.util.Const;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoConfigPluginTest extends TestBase {

    private String intVal = "37";
    private String intListVal = "37,23";
    private long longVal = 21345315436L;
    private boolean boolVal = true;
    private String stringVal = $.randomStr();
    private H.Method enumVal = H.Method.CONNECT;
    private StringValueResolverManager resolverManager;
    private GenieInjector injector;

    @Before
    public void before() throws Exception {
        setup();
        resolverManager = new StringValueResolverManager(mockApp);
        when(mockApp.resolverManager()).thenReturn(resolverManager);
        injector = new GenieInjector(mockApp);
        when(mockApp.injector()).thenReturn(injector);

        GenericPluginManager pluginManager = mock(GenericPluginManager.class);
        Field field = Act.class.getDeclaredField("pluginManager");
        field.setAccessible(true);
        field.set(null, pluginManager);

        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".int")))).thenReturn(intVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".long")))).thenReturn(longVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".bool")))).thenReturn(boolVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".str")))).thenReturn(stringVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".method")))).thenReturn(enumVal);
        when(mockAppConfig.getIgnoreCase(argThat(new ContainsIgnoreCase(".list")))).thenReturn(intListVal);
    }

    @Test
    public void testProvisionConfigData() {
        AutoConfigPlugin.loadPluginAutoConfig(Foo.class, mockApp);
        eq(Integer.parseInt(intVal), Foo.foo_int);
        eq(Integer.parseInt(intVal), Foo.foo_int_val.get());

        eq(longVal, Foo.foo_long);
        eq(longVal, Foo.foo_long_val.get());

        eq(boolVal, Foo.foo_bool);
        eq(boolVal, Foo.foo_bool_val.get());

        eq(stringVal, Foo.foo_str);
        eq(stringVal, Foo.FOO_STR_VAL.get());

        eq(enumVal, Foo.foo_method);
        eq(enumVal, Foo.FOO_METHOD_VAL.get());

        ceq(C.listOf(37, 23), Foo.foo_list);
        ceq(C.listOf(37, 23), Foo.FOO_LIST.get());
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

        static List<Integer> foo_list;
        static final Const<List<Integer>> FOO_LIST = $.constant(null);
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
