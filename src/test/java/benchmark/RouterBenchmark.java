package benchmark;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.Act;
import act.BenchmarkBase;
import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.handler.RequestHandler;
import act.handler.RequestHandlerResolver;
import act.handler.builtin.AlwaysBadRequest;
import act.handler.builtin.AlwaysNotFound;
import act.plugin.GenericPluginManager;
import act.route.*;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.*;
import osgl.ut.TestBase;
import play.Play;
import play.plugins.PluginCollection;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

import static org.osgl.http.H.Method.GET;
import static org.osgl.http.H.Method.POST;

@BenchmarkOptions(warmupRounds = 10, benchmarkRounds = 1000)
public class RouterBenchmark extends BenchmarkBase {

    private static String r24s = S.random(24);
    private static String r21s = S.random(21);
    private static int r20n = N.randInt(20);
    private static String r8s = S.random();

    private static Router router;
    private static RouteTableRouterBuilder builder;
    private ActionContext ctx;
    private static AppConfig config;
    private static App app;

    @BeforeClass
    public static void prepare() throws Exception {
        try {
            Field f = Act.class.getDeclaredField("pluginManager");
            f.setAccessible(true);
            f.set(null, new GenericPluginManager());
        } catch (Exception e) {
            throw E.unexpected(e);
        }
        app = App.testInstance();
        UrlPath.testClassInit();
        config = app.config();
        RequestHandlerResolver controllerLookup = new MockRequestHandlerResolver();
        router = new Router(controllerLookup, app);
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

    void osgl(H.Method method, String url, Object... args) {
        H.Request req = new MockRequest(config, method, url);
        ctx = ActionContext.create(app, req, new MockResponse());
        if (args.length > 0) {
            url = S.fmt(url, args);
        }
        RequestHandler handler = router.getInvoker(method, url, ctx);
        if (AlwaysBadRequest.INSTANCE == handler || AlwaysNotFound.INSTANCE == handler) {
            throw NotFound.get();
        }
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
        runTest(true, true, GET, "/badUrl/whatever/%s/abc/136", r8s);
    }

    @Test
    public void play_InvokeBadUrl() {
        runTest(false, true, GET, "/badUrl/whatever/%s/abc/136", r8s);
    }

    @Test
    public void osgl_HitAtBeginning() {
        runTest(true, GET, "/yemian/%s/", r8s);
    }

    @Test
    public void play_HitAtBeginning() {
        runTest(false, GET, "/yemian/%s/", r8s);
    }

    @Test
    public void osgl_HitAtEnding() {
        runTest(true, GET, "/adm/weixinyingyong/%s/", r8s);
    }

    @Test
    public void play_HitAtEnding() {
        runTest(false, GET, "/adm/weixinyingyong/%s/", r8s);
    }

    @Test
    public void osgl_shortStaticUrl() {
        runTest(true, GET, "/cm");
    }

    @Test
    public void play_shortStaticUrl() {
        runTest(false, GET, "/cm");
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
        runTest(true, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", r24s, r24s, r20n);
    }

    @Test
    public void play_longDynamicUrl() {
        runTest(false, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", r24s, r24s, r20n);
    }


    @Test
    public void osgl_badUrl2() {
        runTest(true, true, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", r24s, r21s, r20n);
    }

    @Test
    public void play_badUrl2() {
        runTest(false, true, POST, "/shuju/tuiguang/%s/mubiao/%s/yemian/%s/remove", r24s, r21s, r20n);
    }

    private void runTest(boolean osgl, H.Method method, String url, Object... fmtArgs) {
        runTest(osgl, false, method, url, fmtArgs);
    }

    private void runTest(boolean osgl, boolean notFoundExpected, H.Method method, String url, Object... fmtArgs) {
        url = S.fmt(url, fmtArgs);
        final int loop = 100 * 100;
        if (osgl) {
            for (int i = 0; i < loop; ++i) {
                try {
                    osgl(method, url);
                    E.unexpectedIf(notFoundExpected, "should raise NotFound here");
                } catch (NotFound e) {
                    E.unexpectedIf(!notFoundExpected, "should not raise NotFound here");
                }
            }
        } else {
            String sMethod = method.name();
            for (int i = 0; i < loop; ++i) {
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
