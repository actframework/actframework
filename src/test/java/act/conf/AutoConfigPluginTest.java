package act.conf;

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

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import act.Act;
import act.ActTestBase;
import act.app.conf.AutoConfig;
import act.app.conf.AutoConfigPlugin;
import act.app.data.StringValueResolverManager;
import act.inject.ActProviders;
import act.inject.genie.GenieFactoryFinder;
import act.inject.genie.GenieInjector;
import act.plugin.GenericPluginManager;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.Const;

import java.lang.reflect.Field;
import java.util.List;

public class AutoConfigPluginTest extends ActTestBase {

    private String intVal = "37";
    private String intListVal = "37,23";
    private long longVal = 21345315436L;
    private boolean boolVal = true;
    private String stringVal = $.randomStr();
    private H.Method enumVal = H.Method.CONNECT;
    private StringValueResolverManager resolverManager;
    private GenieInjector injector;

    @BeforeClass
    public static void classBefore() {
        GenieFactoryFinder.testClassInit();
        ActProviders.testClassInit();
    }

    @Before
    public void before() throws Exception {
        setup();
        resolverManager = new StringValueResolverManager(mockApp);
        when(mockApp.resolverManager()).thenReturn(resolverManager);
        injector = new GenieInjector(mockApp);
        injector.unlock();
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

        @Override
        public boolean matches(Object actual) {
            return actual != null && actual.toString().toUpperCase().contains(substring.toUpperCase());
        }

        public void describeTo(Description description) {
            description.appendText("containsIgnoreCase(\"" + substring + "\")");
        }

    }

}
