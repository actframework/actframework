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

import static act.route.RouteSource.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgl.http.H.Method.GET;

import act.controller.ParamNames;
import act.handler.RequestHandler;
import act.handler.builtin.AlwaysNotFound;
import act.handler.builtin.FileGetter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.$;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.util.C;

import java.io.File;

public class RouterTest extends RouterTestBase {
    private RequestHandler staticDirHandler;

    @BeforeClass
    public static void classInit() {
        UrlPath.testClassInit();
    }

    @Override
    protected void buildRouteMapping(Router router) {
    }

    @Before
    public void prepare() {
        staticDirHandler = Mockito.mock(FileGetter.class);
        when(staticDirHandler.supportPartialPath()).thenReturn(true);
    }

    @Test
    public void testMappingAdded() {
        no(router.isMapped(GET, "/foo"));
        router.addMapping(GET, "/foo", "Foo.bar");
        yes(router.isMapped(GET, "/foo"));
    }

    // #295 Exception using underscore in a URL path variable name
    @Test
    public void testGH295() {
        router.addMapping(GET, "/foo/{var_name}", "Foo.bar");
        yes(router.isMapped(GET, "/foo/{var_name}"));
    }

    // Different URL variable name caused duplicate routes not been reported
    @Test(expected = DuplicateRouteMappingException.class)
    public void testGH561() {
        router.addMapping(GET, "/foo/{a}", "Foo.bar", RouteSource.ACTION_ANNOTATION);
        router.addMapping(GET, "/foo/{b}", "Foo.foo", RouteSource.ACTION_ANNOTATION);
    }

    @Test
    public void testGH561_0() {
        router.addMapping(GET, "/foo/{<[a-z]+>a}", "Foo.bar", RouteSource.ACTION_ANNOTATION);
        router.addMapping(GET, "/foo/{<[0-9]+>b}", "Foo.foo", RouteSource.ACTION_ANNOTATION);
    }

    @Test(expected = DuplicateRouteMappingException.class)
    public void testGH561_extended() {
        router.addMapping(GET, "/foo/{a}/b", "Foo.bar", RouteSource.ACTION_ANNOTATION);
        router.addMapping(GET, "/foo/{b}/b", "Foo.foo", RouteSource.ACTION_ANNOTATION);
    }

    @Test(expected = DuplicateRouteMappingException.class)
    public void testGH561_extended2() {
        router.addMapping(GET, "/foo/{<[0-9]{4}>a}/b", "Foo.bar", RouteSource.ACTION_ANNOTATION);
        router.addMapping(GET, "/foo/{<[0-9]{4}>y}/b", "Foo.foo", RouteSource.ACTION_ANNOTATION);
    }

    @Test
    public void testGH561_extended3() {
        router.addMapping(GET, "/foo/{<[0-9]{5}>a}/b", "Foo.bar", RouteSource.ACTION_ANNOTATION);
        router.addMapping(GET, "/foo/{<[0-9]{4}>y}/b", "Foo.foo", RouteSource.ACTION_ANNOTATION);
    }

    @Test
    public void testGH939_1() {
        router.addMapping(GET, "/foo/~FooBar~", controller, RouteSource.ACTION_ANNOTATION);
        router.getInvoker(GET, "/foo/foo-bar", ctx).handle(ctx);
        controllerInvoked();
    }

    @Test
    public void testGH939_2() {
        router.addMapping(GET, "/foo/~FooBar~/{x}", controller, RouteSource.ACTION_ANNOTATION);
        router.getInvoker(GET, "/foo/foo-bar/xyz", ctx).handle(ctx);
        controllerInvoked();
    }

    @Test
    public void testGH958() {
        router.addMapping(GET, "/foo/~FooBar~/x", controller, RouteSource.ACTION_ANNOTATION);
        router.getInvoker(GET, "/foo/foo-bar/x", ctx).handle(ctx);
        controllerInvoked();
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
        when(ctx.req()).thenReturn(req);
        when(req.path()).thenReturn("/svc/1234");
        router.getInvoker(GET, "/svc/1234/", ctx).handle(ctx);
        controllerInvoked();
        verify(ctx).urlPathParam("id", "1234");
    }

    @Test
    public void searchDynamicUrl2() {
        router.addMapping(GET, "/svc/{<[0-9]{4}>id}-{name}", controller);
        H.Request req = Mockito.mock(H.Request.class);
        when(ctx.req()).thenReturn(req);
        when(req.path()).thenReturn("/svc/1234-abc");
        router.getInvoker(GET, "/svc/1234-abc/", ctx).handle(ctx);
        controllerInvoked();
        verify(ctx).urlPathParam("id", "1234");
        verify(ctx).urlPathParam("name", "abc");
    }

    @Test
    public void searchDynamicUrl3() {
        router.addMapping(GET, "/svc/{<[0-9]{4}>id}-{name}", controller);
        router.addMapping(GET, "/svc/{<[0-9]{4}>sid}-{sname}/obj", controller);
        router.addMapping(GET, "/Persons/Joe/Parents;generations={gen}", controller);
        router.addMapping(GET, "/place/{latitude};{longitude}", controller);

        H.Request req = Mockito.mock(H.Request.class);
        when(ctx.req()).thenReturn(req);

        when(req.path()).thenReturn("/svc/1234-abc");
        when(ctx.urlPath()).thenReturn(UrlPath.of("/svc/1234-abc"));
        router.getInvoker(GET, "/svc/1234-abc", ctx).handle(ctx);
        verify(ctx).urlPathParam("id", "1234");
        verify(ctx).urlPathParam("name", "abc");

        when(req.path()).thenReturn("/svc/1234-abc/obj");
        when(ctx.urlPath()).thenReturn(UrlPath.of("/svc/1234-abc/obj"));
        router.getInvoker(GET, "/svc/1234-abc/obj", ctx).handle(ctx);
        verify(ctx).urlPathParam("sid", "1234");
        verify(ctx).urlPathParam("sname", "abc");

        when(req.path()).thenReturn("/Persons/Joe/Parents;generations=147");
        when(ctx.urlPath()).thenReturn(UrlPath.of("/Persons/Joe/Parents;generations=147"));
        router.getInvoker(GET, "/Persons/Joe/Parents;generations=147", ctx).handle(ctx);
        verify(ctx).urlPathParam("gen", "147");

        when(req.path()).thenReturn("/place/39.87381;-86.1399");
        when(ctx.urlPath()).thenReturn(UrlPath.of("/place/39.87381;-86.1399"));
        router.getInvoker(GET, "/place/39.87381;-86.1399", ctx).handle(ctx);
        verify(ctx).urlPathParam("latitude", "39.87381");
        verify(ctx).urlPathParam("longitude", "-86.1399");
    }

    @Test
    public void regExtStyleA() {
        _regExtTests("n:[0-9]+");
    }

    @Test
    public void regExtStyleB2() {
        _regExtTests("{n<[0-9]+>}");
    }

    private void _regExtTests(String pattern) {
        router.addMapping(GET, "/int/" + pattern, controller);
        H.Request req = Mockito.mock(H.Request.class);
        when(ctx.req()).thenReturn(req);

        when(req.path()).thenReturn("/int/33");
        RequestHandler handler = router.getInvoker(GET, "/int/33", ctx);
        same(controller, handler);

        verify(ctx).urlPathParam("n", "33");
    }

    @Test
    public void searchPathEndsWithIgnoreNotation() {
        router.addMapping(GET, "/foo/bar/...", controller);
        router.addMapping(GET, "/svc/{id}/...", controller);

        H.Request req = Mockito.mock(H.Request.class);
        when(ctx.req()).thenReturn(req);

        when(req.path()).thenReturn("/foo/bar/something-should-be-ignored");
        RequestHandler handler = router.getInvoker(GET, "/foo/bar/something-should-be-ignored", ctx);
        same(controller, handler);

        when(req.path()).thenReturn("/svc/123/another-thing-should-be-ignored/and-whatever/else");
        router.getInvoker(GET, "/svc/123/another-thing-should-be-ignored/and-whatever/else", ctx).handle(ctx);
        verify(ctx).urlPathParam("id", "123");
    }

    @Test(expected = ConfigurationException.class)
    public void routingConfiguredWithEndStarConflict1() {
        router.addMapping(GET, "/foo/bar/...", controller);
        router.addMapping(GET, "/foo/bar/xyz", controller);
    }

    @Test(expected = ConfigurationException.class)
    public void routingConfiguredWithEndStarConflict2() {
        router.addMapping(GET, "/foo/bar/xyz", controller);
        router.addMapping(GET, "/foo/bar/...", controller);
    }

    @Test
    public void searchPartialUrl() {
        router.addMapping(GET, "/public", staticDirHandler);
        router.getInvoker(GET, "/public/foo/bar.txt", ctx).handle(ctx);
        verify(staticDirHandler).handle(ctx);
        verify(ctx).param(ParamNames.PATH, "foo/bar.txt");
    }

    @Test
    public void routeWithStaticDir() {
        router.addMapping(GET, "/public", "file:/public");
        RequestHandler handler = router.getInvoker(GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof FileGetter);
        yes(handler.supportPartialPath());
        eq(new File(BASE, "/public"), fieldVal(handler, "base"));
    }

    @Test
    public void overrideExistingRouting() {
        routeWithStaticDir();
        router.addMapping(GET, "/public", "file:/private");
        RequestHandler handler = router.getInvoker(GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof FileGetter);
        yes(handler.supportPartialPath());
        eq(new File(BASE, "/private"), fieldVal(handler, "base"));
    }

    @Test
    public void doNotOverrideExistingRouting() {
        routeWithStaticDir();
        router.addMapping(GET, "/public", "file:/private", ACTION_ANNOTATION);
        RequestHandler handler = router.getInvoker(GET, "/public/foo/bar.txt", ctx);
        yes(handler instanceof FileGetter);
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
        router.addMapping(GET, "/foo/{id}", controller);
        router.addMapping(GET, "/foo/{id}/bar", controller);
        eq(controller, router.getInvoker(GET, "/foo/123", ctx));
        eq(controller, router.getInvoker(GET, "/foo/something/bar", ctx));
    }

    @Test(expected = DuplicateRouteMappingException.class)
    public void itShallNotAllowAddingHandlersToSameRouteEndingWithDynamicPart() {
        router.addMapping(GET, "/foo/{id}", "Controller.foo", ACTION_ANNOTATION);
        router.addMapping(GET, "/foo/{id}", "Foo.bar", ACTION_ANNOTATION);
    }

    @Test
    public void testReverseRoute() {
        router.addMapping(GET, "/foo/bar", "pkg.Foo.bar");
        eq(router.reverseRoute("pkg.Foo.bar"), "/foo/bar");
        eq(router.reverseRoute("pkg.Foo.bar", C.<String, Object>Map("foo", "bar")), "/foo/bar?foo=bar");
    }

    @Test
    public void testReverseRouteWithPathVar() {
        router.addMapping(GET, "/foo/{foo}", "pkg.Foo.foo");
        router.addMapping(GET, "/foo/{fooId}/bar/{barId}", "pkg.Foo.bar");
        eq(router.reverseRoute("pkg.Foo.bar"), "/foo/-/bar/-");
        eq(router.reverseRoute("pkg.Foo.bar", C.<String, Object>Map("fooId", 1, "barId", 3)), "/foo/1/bar/3");
        eq(router.reverseRoute("pkg.Foo.foo", C.<String, Object>Map("foo", 1)), "/foo/1");
    }

    @Test
    public void testReverseRouteWithPathVar2() {
        router.addMapping(GET, "/foo/{fooId}-{barId}", "pkg.Foo.bar");
        router.addMapping(GET, "/foo/{foo}.htm", "pkg.Foo.foo");
        //eq(router.reverseRoute("pkg.Foo.bar"), "/foo/---");
        eq(router.reverseRoute("pkg.Foo.bar", C.<String, Object>Map("fooId", 1, "barId", 3)), "/foo/1-3");
        eq(router.reverseRoute("pkg.Foo.foo", C.<String, Object>Map("foo", 1)), "/foo/1.htm");
    }

    @Test
    public void testInferFullActionPath() {
        final String currentActionPath = "com.my.comp.proj_a.controller.MyController.login";
        $.Func0<String> provider = new $.Func0<String>() {
            @Override
            public String apply() throws NotAppliedException, $.Break {
                return currentActionPath;
            }
        };
        eq("com.my.comp.proj_a.controller.MyController.home", Router.inferFullActionPath("home", provider));
        eq("com.my.comp.proj_a.controller.YourController.home", Router.inferFullActionPath("YourController.home", provider));
        eq("pkg.YourController.home", Router.inferFullActionPath("pkg.YourController.home", provider));
    }

    @Test
    public void ghIssue121() {
        router.addMapping(GET, "/abc/134/foo/{a}", "pkg.Foo.any", ACTION_ANNOTATION);
        router.addMapping(GET, "/abc/134/foo/bar", "pkg.Foo.bar", ACTION_ANNOTATION);
        router.addMapping(GET, "/abc/134/foo/zee", "pkg.Foo.zee", ACTION_ANNOTATION);
        eq("/abc/134/foo/abc", router.reverseRoute("pkg.Foo.any", C.<String, Object>Map("a", "abc")));
        eq("/abc/134/foo/bar", router.reverseRoute("pkg.Foo.bar"));
        eq("/abc/134/foo/zee", router.reverseRoute("pkg.Foo.zee"));
    }

}
