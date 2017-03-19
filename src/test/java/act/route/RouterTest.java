package act.route;

import act.controller.ParamNames;
import act.handler.RequestHandler;
import act.handler.builtin.AlwaysNotFound;
import act.handler.builtin.StaticFileGetter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.util.C;

import java.io.File;

import static act.route.RouteSource.*;
import static org.osgl.http.H.Method.GET;

public class RouterTest extends RouterTestBase {
    private RequestHandler staticDirHandler;

    @Override
    protected void buildRouteMapping(Router router) {
    }

    @Before
    public void prepare() {
        staticDirHandler = Mockito.mock(StaticFileGetter.class);
        Mockito.when(staticDirHandler.supportPartialPath()).thenReturn(true);
    }

    @Test
    public void testMappingAdded() {
        no(router.isMapped(GET, "/foo"));
        router.addMapping(GET, "/foo", "Foo.bar");
        yes(router.isMapped(GET, "/foo"));
    }

    @Test
    public void searchRoot() {
        router.addMapping(GET, "/", controller);
        router.getInvoker(GET, "/", ctx).handle(ctx);
        controllerInvoked();
    }

    @Test
    public void searchBadUrl() {
        RequestHandler handler = router.getInvoker(GET, "/nonexists", ctx);
        same(handler, AlwaysNotFound.INSTANCE);
    }

    @Test
    public void searchStaticUrl() {
        router.addMapping(H.Method.POST, "/foo/bar", controller);
        router.getInvoker(H.Method.POST, "/foo/bar", ctx).handle(ctx);
        controllerInvoked();
    }

    @Test
    public void searchDynamicUrl() {
        router.addMapping(GET, "/svc/{<[0-9]{4}>id}", controller);
        H.Request req = Mockito.mock(H.Request.class);
        Mockito.when(ctx.req()).thenReturn(req);
        Mockito.when(req.path()).thenReturn("/svc/1234");
        router.getInvoker(GET, "/svc/1234/", ctx).handle(ctx);
        controllerInvoked();
        Mockito.verify(ctx).param("id", "1234");
    }

    @Test
    public void searchPartialUrl() {
        router.addMapping(GET, "/public", staticDirHandler);
        router.getInvoker(GET, "/public/foo/bar.txt", ctx).handle(ctx);
        Mockito.verify(staticDirHandler).handle(ctx);
        Mockito.verify(ctx).param(ParamNames.PATH, "/foo/bar.txt");
    }

    @Test
    public void routeWithStaticDir() {
        router.addMapping(GET, "/public", "file:/public");
        RequestHandler handler = router.getInvoker(GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof StaticFileGetter);
        yes(handler.supportPartialPath());
        eq(new File(BASE, "/public"), fieldVal(handler, "base"));
    }

    @Test
    public void overrideExistingRouting() {
        routeWithStaticDir();
        router.addMapping(GET, "/public", "file:/private");
        RequestHandler handler = router.getInvoker(GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof StaticFileGetter);
        yes(handler.supportPartialPath());
        eq(new File(BASE, "/private"), fieldVal(handler, "base"));
    }

    @Test
    public void doNotOverrideExistingRouting() {
        routeWithStaticDir();
        router.addMapping(GET, "/public", "file:/private", ACTION_ANNOTATION);
        RequestHandler handler = router.getInvoker(GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof StaticFileGetter);
        yes(handler.supportPartialPath());
        eq(new File(BASE, "/public"), fieldVal(handler, "base"));
    }

    @Test
    public void senseControllerMethodWithoutControllerPackage() {
        router.addMapping(GET, "/foo", "Controller.foo");
        no(router.isActionMethod("foo.controller.Controller", "foo"));
        yes(router.isActionMethod("Controller", "foo"));

        router.addMapping(GET, "/bar", "com.newcontroller.Controller.bar");
        yes(router.isActionMethod("com.newcontroller.Controller", "bar"));
    }

    @Test
    public void itShallNotOverwriteRouteMappingWithSameRouteSource() {
        for (RouteSource source : RouteSource.values()) {
            router = new Router(controllerLookup, app);
            router.addMapping(GET, "/foo", "Controller.foo", source);
            try {
                router.addMapping(GET, "/foo", "Foo.bar", source);
                if (source != ROUTE_TABLE && source != ADMIN_OVERWRITE) {
                    fail("expected DuplicateRouteMappingException");
                }
            } catch (DuplicateRouteMappingException e) {
                // good
            }
        }
    }

    @Test
    public void testAddingTwoRoutesWithSameDynamicPart() {
        router.addMapping(GET, "/foo/{id}", "Controller.foo");
        router.addMapping(GET, "/foo/{id}/bar", "Foo.bar");
    }

    @Test(expected = DuplicateRouteMappingException.class)
    public void itShallNotAllowAddingHandlersToSameRouteEndingWithDynamicPart() {
        router.addMapping(GET, "/foo/{id}", "Controller.foo", RouteSource.ACTION_ANNOTATION);
        router.addMapping(GET, "/foo/{id}", "Foo.bar", RouteSource.ACTION_ANNOTATION);
    }

    @Test
    public void testReverseRoute() {
        router.addMapping(GET, "/foo/bar", "pkg.Foo.bar");
        eq(router.reverseRoute("pkg.Foo.bar"), "/foo/bar");
        eq(router.reverseRoute("pkg.Foo.bar", C.<String, Object>map("foo", "bar")), "/foo/bar?foo=bar");
    }

    @Test
    public void testReverseRouteWithPathVar() {
        router.addMapping(GET, "/foo/{fooId}/bar/{barId}", "pkg.Foo.bar");
        eq(router.reverseRoute("pkg.Foo.bar"), "/foo/{fooId}/bar/{barId}");
        eq(router.reverseRoute("pkg.Foo.bar", C.<String, Object>map("fooId", 1, "barId", 3)), "/foo/1/bar/3");
    }

    @Test
    public void testInferFullActionPath() {
        final String currentActionPath = "com.my.comp.proj_a.controller.MyController.login";
        $.Func0<String> provider = new $.Func0<String>() {
            @Override
            public String apply() throws NotAppliedException, Osgl.Break {
                return currentActionPath;
            }
        };
        eq("com.my.comp.proj_a.controller.MyController.home", Router.inferFullActionPath("home", provider));
        eq("com.my.comp.proj_a.controller.YourController.home", Router.inferFullActionPath("YourController.home", provider));
        eq("pkg.YourController.home", Router.inferFullActionPath("pkg.YourController.home", provider));
    }

}
