package org.osgl.mvc.server.route;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.server.ParamNames;
import org.osgl.mvc.server.action.ActionHandler;
import org.osgl.mvc.server.action.builtin.StaticFileGetter;

public class RouterTest extends RouterTestBase {
    private ActionHandler staticDirHandler;

    @Override
    protected void buildRouteMapping(Router router) {
    }

    @Before
    public void prepare() {
        staticDirHandler = Mockito.mock(StaticFileGetter.class);
        Mockito.when(staticDirHandler.supportPartialPath()).thenReturn(true);
    }

    @Test
    public void searchRoot() {
        router.addRouteMapping(H.Method.GET, "/", controller);
        router.getInvoker(H.Method.GET, "/", ctx).invoke(ctx);
        controllerInvoked();
    }

    @Test(expected = NotFound.class)
    public void searchBadUrl() {
        router.getInvoker(H.Method.GET, "/nonexists", ctx);
    }

    @Test
    public void searchStaticUrl() {
        router.addRouteMapping(H.Method.POST, "/foo/bar", controller);
        router.getInvoker(H.Method.POST, "/foo/bar", ctx).invoke(ctx);
        controllerInvoked();
    }

    @Test
    public void searchDynamicUrl() {
        router.addRouteMapping(H.Method.GET, "/svc/{<[0-9]{4}>id}", controller);
        router.getInvoker(H.Method.GET, "/svc/1234/", ctx).invoke(ctx);
        controllerInvoked();
        Mockito.verify(ctx).param("id", "1234");
    }

    @Test
    public void searchPartialUrl() {
        router.addRouteMapping(H.Method.GET, "/public", staticDirHandler);
        router.getInvoker(H.Method.GET, "/public/foo/bar.txt", ctx).invoke(ctx);
        Mockito.verify(staticDirHandler).invoke(ctx);
        Mockito.verify(ctx).param(ParamNames.PATH, "/foo/bar.txt");
    }

    @Test
    public void routeWithStaticDir() {
        router.addRouteMapping(H.Method.GET, "/public", "staticDir:/public");
        ActionHandler handler = router.getInvoker(H.Method.GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof StaticFileGetter);
        yes(handler.supportPartialPath());
        eq("/public", fieldVal(handler, "base"));
    }

}
