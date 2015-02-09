package org.osgl.oms.route;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.ParamNames;
import org.osgl.oms.action.ActionHandler;
import org.osgl.oms.action.builtin.StaticFileGetter;

import java.util.Map;
import java.util.Properties;

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
        router.addMapping(H.Method.GET, "/", controller);
        router.getInvoker(H.Method.GET, "/", ctx).invoke(ctx);
        controllerInvoked();
    }

    @Test(expected = NotFound.class)
    public void searchBadUrl() {
        router.getInvoker(H.Method.GET, "/nonexists", ctx);
    }

    @Test
    public void searchStaticUrl() {
        router.addMapping(H.Method.POST, "/foo/bar", controller);
        router.getInvoker(H.Method.POST, "/foo/bar", ctx).invoke(ctx);
        controllerInvoked();
    }

    @Test
    public void searchDynamicUrl() {
        router.addMapping(H.Method.GET, "/svc/{<[0-9]{4}>id}", controller);
        router.getInvoker(H.Method.GET, "/svc/1234/", ctx).invoke(ctx);
        controllerInvoked();
        Mockito.verify(ctx).param("id", "1234");
    }

    @Test
    public void searchPartialUrl() {
        router.addMapping(H.Method.GET, "/public", staticDirHandler);
        router.getInvoker(H.Method.GET, "/public/foo/bar.txt", ctx).invoke(ctx);
        Mockito.verify(staticDirHandler).invoke(ctx);
        Mockito.verify(ctx).param(ParamNames.PATH, "/foo/bar.txt");
    }

    @Test
    public void routeWithStaticDir() {
        router.addMapping(H.Method.GET, "/public", "staticDir:/public");
        ActionHandler handler = router.getInvoker(H.Method.GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof StaticFileGetter);
        yes(handler.supportPartialPath());
        eq("/public", fieldVal(handler, "base"));
    }

    @Test
    public void overrideExistingRouting() {
        routeWithStaticDir();
        router.addMapping(H.Method.GET, "/public", "staticDir:/private");
        ActionHandler handler = router.getInvoker(H.Method.GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof StaticFileGetter);
        yes(handler.supportPartialPath());
        eq("/private", fieldVal(handler, "base"));
    }

    @Test
    public void doNotOverrideExistingRouting() {
        routeWithStaticDir();
        router.addMappingIfNotMapped(H.Method.GET, "/public", "staticDir:/private");
        ActionHandler handler = router.getInvoker(H.Method.GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof StaticFileGetter);
        yes(handler.supportPartialPath());
        eq("/public", fieldVal(handler, "base"));
    }

    @Test
    public void senseControllerMethodWithControllerPackage() {
        Properties p = new Properties();
        p.setProperty("controller_package", "foo.controller");
        router = new Router(controllerLookup, new AppConfig((Map)p));

        router.addMapping(H.Method.GET, "/foo", "Controller.foo");
        yes(router.isActionMethod("foo.controller.Controller", "foo"));

        router.addMapping(H.Method.GET, "/bar", "com.newcontroller.Controller.bar");
        yes(router.isActionMethod("com.newcontroller.Controller", "bar"));
    }

    @Test
    public void senseControllerMethodWithoutControllerPackage() {
        router.addMapping(H.Method.GET, "/foo", "Controller.foo");
        no(router.isActionMethod("foo.controller.Controller", "foo"));
        yes(router.isActionMethod("Controller", "foo"));

        router.addMapping(H.Method.GET, "/bar", "com.newcontroller.Controller.bar");
        yes(router.isActionMethod("com.newcontroller.Controller", "bar"));
    }

}
