package org.osgl.mvc.server.route;

import org.junit.Before;
import org.mockito.Mockito;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.TestBase;
import org.osgl.mvc.server.action.ActionHandler;
import org.osgl.mvc.server.action.ActionHandlerResolver;

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
        router = new Router(controllerLookup);
        buildRouteMapping(router);
        ctx = Mockito.mock(AppContext.class);
    }

    protected void provisionControllerLookup(ActionHandlerResolver controllerLookup) {
        when(controllerLookup.resolve(anyString())).thenReturn(controller);
    }

    protected abstract void buildRouteMapping(Router router);

    protected void controllerInvoked() {
        verify(controller).invoke(ctx);
    }

}
