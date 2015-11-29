package act.route;

import act.BenchmarkBase;
import act.TestBase;
import act.app.ActionContext;
import act.app.App;
import act.handler.RequestHandlerResolver;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.*;
import play.Play;
import play.plugins.PluginCollection;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.osgl.http.H.Method.GET;
import static org.osgl.http.H.Method.POST;

@BenchmarkOptions(warmupRounds = 1, benchmarkRounds = 10)
@Ignore
public class RouterBenchmark extends BenchmarkBase {

    private Router router;
    private RouteTableRouterBuilder builder;
    private ActionContext ctx;

    public RouterBenchmark() {
        RequestHandlerResolver controllerLookup = new MockRequestHandlerResolver();
        router = new Router(controllerLookup, Mockito.mock(App.class));
        InputStream is = TestBase.class.getResourceAsStream("/routes");
        String fc = IO.readContentAsString(is);
        builder = new RouteTableRouterBuilder(fc.split("[\r\n]+"));
        builder.build(router);
        Play.pluginCollection = new PluginCollection();
        URL url = TestBase.class.getResource("/routes");
        Play.applicationPath = new File(FastStr.of(url.getPath()).beforeLast('/').toString());
        Play.routes = VirtualFile.fromRelativePath("routes");
        play.mvc.Router.load("");
    }

    @Before
    public void prepare() {
        ctx = ActionContext.create(mock(App.class), mock(H.Request.class), mock(H.Response.class));
    }

    void osgl(H.Method method, String url, Object... args) {
        if (args.length > 0) {
            url = S.fmt(url, args);
        }
        router.getInvoker(method, url, ctx);
    }

    void play(String method, String url, Object... args) {
        if (args.length > 0) {
            url = S.fmt(url, args);
        }
        for (play.mvc.Router.Route route : play.mvc.Router.routes) {
            Map<String, String> actArgs = route.matches(method, url, null, null);
            if (actArgs != null) {
                if (route.action.indexOf("{") > -1) { // more optimization ?
                    for (String arg : actArgs.keySet()) {
                        route.action = route.action.replace("{" + arg + "}", actArgs.get(arg));
                    }
                }
                if (route.action.equals("404")) {
                    throw new NotFound(route.path);
                }
                return;
            }
        }
        throw new NotFound(url);
    }

    @Test
    public void osgl_InvokeBadUrl() {
        runTest(true, true, GET, "/badUrl/whatever/%s/abc/136", S.random());
    }

    @Test
    public void play_InvokeBadUrl() {
        runTest(false, true, GET, "/badUrl/whatever/%s/abc/136", S.random());
    }

    @Test
    public void osgl_HitAtBeginning() {
        runTest(true, GET, "/yemian/%s/", S.random());
    }

    @Test
    public void play_HitAtBeginning() {
        runTest(false, GET, "/yemian/%s/", S.random());
    }

    @Test
    public void osgl_HitAtEnding() {
        runTest(true, GET, "/adm/weixinyingyong/%s/", S.random());
    }

    @Test
    public void play_HitAtEnding() {
        runTest(false, GET, "/adm/weixinyingyong/%s/", S.random());
    }

    @Test
    public void osgl_longStaticUrl() {
        runTest(true, POST, "/shuju/yonghu/guanliYonghu/shanchu");
    }

    @Test
    public void play_longStaticUrl() {
        runTest(false, POST, "/shuju/yonghu/guanliYonghu/shanchu");
    }

    @Test
    public void osgl_longDynamicUrl() {
        runTest(true, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", S.random(24), S.random(24), N.randInt(20));
    }

    @Test
    public void play_longDynamicUrl() {
        runTest(false, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", S.random(24), S.random(24), N.randInt(20));
    }


    @Test
    public void osgl_badUrl2() {
        runTest(true, true, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", S.random(24), S.random(21), N.randInt(20));
    }

    @Test
    public void play_badUrl2() {
        runTest(false, true, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", S.random(24), S.random(21), N.randInt(20));
    }

    private void runTest(boolean osgl, H.Method method, String url, Object... fmtArgs) {
        runTest(osgl, false, method, url, fmtArgs);
    }

    private void runTest(boolean osgl, boolean notFoundExpected, H.Method method, String url, Object... fmtArgs) {
        url = S.fmt(url, fmtArgs);
        if (osgl) {
            for (int i = 0; i < 1000 * 100; ++i) {
                try {
                    osgl(method, url);
                    E.unexpectedIf(notFoundExpected, "should raise NotFound here");
                } catch (NotFound e) {
                    E.unexpectedIf(!notFoundExpected, "should not raise NotFound here");
                }
            }
        } else {
            String sMethod = method.name();
            for (int i = 0; i < 1000 * 100; ++i) {
                try {
                    play(sMethod, url);
                    E.unexpectedIf(notFoundExpected, "should raise NotFound here");
                } catch (NotFound e) {
                    E.unexpectedIf(!notFoundExpected, "should not raise NotFound here");
                }
            }
        }
    }

}
