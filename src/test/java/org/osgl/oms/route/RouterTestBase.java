package org.osgl.oms.route;

import org.junit.Before;
import org.mockito.Mockito;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.AppContext;
import org.osgl.oms.TestBase;
import org.osgl.oms.action.ActionHandler;
import org.osgl.oms.action.ActionHandlerResolver;

import static org.mockito.Mockito.*;

public abstract class RouterTestBase extends TestBase {

    protected Router router;
    protected ActionHandler controller;
    protected ActionHandlerResolver controllerLookup;
    protected AppContext ctx;

    @Before
    public void _prepare() {
        controller = mock(NamedMockHandler.class);
        controllerLookup = mock(ActionHandlerResolver.class);
        provisionControllerLookup(controllerLookup);
        router = new Router(controllerLookup, appConfig());
        buildRouteMapping(router);
        ctx = Mockito.mock(AppContext.class);
    }

    protected void provisionControllerLookup(ActionHandlerResolver controllerLookup) {
        when(controllerLookup.resolve(anyString())).thenReturn(controller);
    }

    protected abstract void buildRouteMapping(Router router);

    protected AppConfig appConfig() {
        return new AppConfig();
    }

    protected void controllerInvoked() {
        verify(controller).invoke(ctx);
    }

}
