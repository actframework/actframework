package org.osgl.oms.route;

import org.junit.Before;
import org.mockito.Mockito;
import org.osgl.oms.app.App;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.TestBase;
import org.osgl.oms.handler.RequestHandler;
import org.osgl.oms.handler.RequestHandlerResolver;

import static org.mockito.Mockito.*;

public abstract class RouterTestBase extends TestBase {

    protected Router router;
    protected RequestHandler controller;
    protected RequestHandlerResolver controllerLookup;
    protected AppContext ctx;

    @Before
    public void _prepare() {
        controller = mock(NamedMockHandler.class);
        controllerLookup = mock(RequestHandlerResolver.class);
        provisionControllerLookup(controllerLookup);
        AppConfig config = appConfig();
        App app = mock(App.class);
        when(app.config()).thenReturn(config);
        router = new Router(controllerLookup, app);
        buildRouteMapping(router);
        ctx = mock(AppContext.class);
        when(ctx.app()).thenReturn(app);
    }

    protected void provisionControllerLookup(RequestHandlerResolver controllerLookup) {
        when(controllerLookup.resolve(anyString())).thenReturn(controller);
    }

    protected abstract void buildRouteMapping(Router router);

    protected AppConfig appConfig() {
        return new AppConfig();
    }

    protected void controllerInvoked() {
        verify(controller).handle(ctx);
    }

}
