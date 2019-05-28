package act.route;

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

import static org.mockito.Mockito.*;

import act.ActTestBase;
import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.handler.RequestHandler;
import act.handler.RequestHandlerResolver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl.util.S;

import java.io.File;
import java.lang.reflect.Field;

public abstract class RouterTestBase extends ActTestBase {

    protected Router router;
    protected RequestHandler controller;
    protected RequestHandlerResolver controllerLookup;
    protected ActionContext ctx;
    protected static App app;
    protected static final File BASE = new File("target/test-classes");

    @BeforeClass
    public static void prepareClass() throws Exception {
        AppConfig config = appConfig();
        app = mock(App.class);
        when(app.config()).thenReturn(config);
        when(app.cuid()).thenReturn(S.random());
        when(app.file(anyString())).thenAnswer(new Answer<File>() {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable {
                String path = (String) invocation.getArguments()[0];
                return new File(BASE, path);
            }
        });
        Field f = App.class.getDeclaredField("INST");
        f.setAccessible(true);
        f.set(null, app);
    }

    @Before
    public void _prepare() {
        controller = mock(NamedMockHandler.class);
        controllerLookup = mock(RequestHandlerResolver.class);
        provisionControllerLookup(controllerLookup);
        router = new Router(controllerLookup, app);
        buildRouteMapping(router);
        ctx = mock(ActionContext.class);
        when(ctx.router()).thenReturn(router);
        when(ctx.app()).thenReturn(app);
        doCallRealMethod().when(ctx).proceedWithHandler(any(RequestHandler.class));
    }

    protected void provisionControllerLookup(RequestHandlerResolver controllerLookup) {
        when(controllerLookup.resolve(anyString(), any(App.class))).thenReturn(controller);
    }

    protected abstract void buildRouteMapping(Router router);

    protected static AppConfig appConfig() {
        return new AppConfig();
    }

    protected void controllerInvoked() {
        verify(controller).handle(ctx);
    }

}
