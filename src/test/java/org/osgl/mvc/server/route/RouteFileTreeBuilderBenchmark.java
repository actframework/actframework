package org.osgl.mvc.server.route;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.Before;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.http.util.Path;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.BenchmarkBase;
import org.osgl.mvc.server.MockAppContext;
import org.osgl.mvc.server.TestBase;
import org.osgl.util.*;
import play.Play;
import play.mvc.Router;
import play.plugins.PluginCollection;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.osgl.http.H.Method.GET;
import static org.osgl.http.H.Method.POST;

@BenchmarkOptions(warmupRounds = 1, benchmarkRounds = 10)
public class RouteFileTreeBuilderBenchmark extends BenchmarkBase {

    private Tree tree;
    private RouteFileTreeBuilder builder;
    private AppContext ctx;

    public RouteFileTreeBuilderBenchmark() {
        tree = new Tree();
        InputStream is = TestBase.class.getResourceAsStream("/routes");
        String fc = IO.readContentAsString(is);
        builder = new RouteFileTreeBuilder(fc.split("[\r\n]+"));
        builder.setInvokerResolver(new MockActionInvokerResolver());
        builder.build(tree);
        Play.pluginCollection = new PluginCollection();
        URL url = TestBase.class.getResource("/routes");
        Play.applicationPath = new File(FastStr.of(url.getPath()).beforeLast('/').toString());
        Play.routes = VirtualFile.fromRelativePath("routes");
        Router.load("");
    }

    @Before
    public void prepare() {
        ctx = new MockAppContext();
    }

    void osgl(H.Method method, String url, Object... args) {
        List<CharSequence> path = Path.tokenize(Unsafe.bufOf(S.fmt(url, args)));
        tree.getInvoker(method, path, ctx);
    }

    void play(String method, String url, Object... fmtArgs) {
        url = S.fmt(url, fmtArgs);
        for (Router.Route route : Router.routes) {
            Map<String, String> args = route.matches(method, url, null, null);
            if (args != null) {
                if (route.action.indexOf("{") > -1) { // more optimization ?
                    for (String arg : args.keySet()) {
                        route.action = route.action.replace("{" + arg + "}", args.get(arg));
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
        runTest(true, GET, "/badUrl/whatever/%s/abc/136", S.random());
    }

    @Test
    public void play_InvokeBadUrl() {
        runTest(false, GET, "/badUrl/whatever/%s/abc/136", S.random());
    }

    @Test
    public void osgl_HitAtBeginning() {
        runTest(true, GET, "/page/%s/", S.random());
    }

    @Test
    public void play_HitAtBeginning() {
        runTest(false, GET, "/page/%s/", S.random());
    }

    @Test
    public void osgl_HitAtEndding() {
        runTest(true, GET, "/su/fbapp/%s/", S.random());
    }

    @Test
    public void play_HitAtEndding() {
        runTest(false, GET, "/su/fbapp/%s/", S.random());
    }

    @Test
    public void osgl_longStaticUrl() {
        runTest(true, POST, "/data/client/adminUser/remove");
    }

    @Test
    public void play_longStaticUrl() {
        runTest(false, POST, "/data/client/adminUser/remove");
    }

    @Test
    public void osgl_longDynamicUrl() {
        runTest(true, POST, "/data/campaign/%s/target/%s/page/%s/remove", S.random(24), S.random(24), N.randInt(20));
    }

    @Test
    public void play_longDynamicUrl() {
        runTest(false, POST, "/data/campaign/%s/target/%s/page/%s/remove", S.random(24), S.random(24), N.randInt(20));
    }


    @Test
    public void osgl_badUrl2() {
        runTest(true, POST, "/data/campaign/%s/target/%s/page/%s/remove", S.random(24), S.random(21), N.randInt(20));
    }

    @Test
    public void play_badUrl2() {
        runTest(false, POST, "/data/campaign/%s/target/%s/page/%s/remove", S.random(24), S.random(21), N.randInt(20));
    }

    private void runTest(boolean osgl, H.Method method, String url, Object... fmtArgs) {
        url = S.fmt(url, fmtArgs);
        if (osgl) {
            for (int i = 0; i < 1000 * 100; ++i) {
                try {
                    osgl(method, url);
                } catch (NotFound e) {
                    // ignore
                }
            }
        } else {
            String sMethod = method.name();
            for (int i = 0; i < 1000 * 100; ++i) {
                try {
                    play(sMethod, url);
                } catch (NotFound e) {
                    // ignore
                }
            }
        }
    }

}
