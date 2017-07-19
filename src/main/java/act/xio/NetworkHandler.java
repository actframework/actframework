package act.xio;

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

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.app.event.AppEventId;
import act.app.util.NamedPort;
import act.handler.RequestHandler;
import act.handler.builtin.AlwaysNotFound;
import act.handler.builtin.StaticFileGetter;
import act.handler.builtin.StaticResourceGetter;
import act.handler.builtin.controller.FastRequestHandler;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.metric.Metric;
import act.metric.MetricInfo;
import act.metric.Timer;
import act.route.Router;
import act.util.DestroyableBase;
import act.view.ActErrorResult;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Result;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * A `NetworkHandler` can be registered to an {@link Network} and get invoked when
 * there are network event (e.g. an HTTP request) incoming
 */
public class NetworkHandler extends DestroyableBase {

    private static Logger logger = LogManager.get(NetworkHandler.class);

    final private App app;
    private NamedPort port;
    private Metric metric;
    private $.Func2<H.Request, String, String> contentSuffixProcessor;
    private $.Func2<H.Request, String, String> urlContextProcessor;

    public NetworkHandler(App app) {
        E.NPE(app);
        this.app = app;
        this.metric = Act.metricPlugin().metric("act.http");
        this.initUrlProcessors();
        app.registerHotReloadListener(new App.HotReloadListener() {
            @Override
            public void preHotReload() {
                initUrlProcessors();
            }
        });
    }

    private void initUrlProcessors() {
        this.contentSuffixProcessor = app.config().contentSuffixAware() ? new ContentSuffixSensor() : DUMB_CONTENT_SUFFIX_SENSOR;
        String urlContext = app.config().urlContext();
        if (null != urlContext) {
            this.urlContextProcessor = new UrlContextProcessor(urlContext);
        } else {
            this.urlContextProcessor = DUMB_CONTENT_SUFFIX_SENSOR;
        }
    }

    public NetworkHandler(App app, NamedPort port) {
        this(app);
        this.port = port;
    }

    public App app() {
        return app;
    }

    public void handle(final ActionContext ctx, final NetworkDispatcher dispatcher) {
        if (isDestroyed()) {
            return;
        }
        Exception refreshError = null;
        if (Act.isDev()) {
            try {
                boolean updated = app.checkUpdates(false);
                if (updated) {
                    app.jobManager().on(AppEventId.POST_START, new Runnable() {
                        @Override
                        public void run() {
                            handle(ctx, dispatcher);
                        }
                    }, true);
                    dispatcher.keep();
                    return;
                }
            } catch (Exception e) {
                refreshError = e;
            }
        }
        final H.Request req = ctx.req();
        String url = req.url();
        H.Method method = req.method();
        url = contentSuffixProcessor.apply(req, url);
        try {
            url = urlContextProcessor.apply(req, url);
        } catch (NotFound notFound) {
            ctx.handler(AlwaysNotFound.INSTANCE);
            ctx.saveLocal();
            AlwaysNotFound.INSTANCE.apply(ctx);
            return;
        }
        Timer timer = metric.startTimer(MetricInfo.ROUTING);
        final RequestHandler requestHandler = router().getInvoker(method, url, ctx);
        ctx.handler(requestHandler);
        timer.stop();
        boolean resourceGetter = requestHandler instanceof StaticResourceGetter || requestHandler instanceof StaticFileGetter;
        if (null != refreshError && !resourceGetter) {
            ctx.saveLocal();
            handleException(refreshError, ctx, "Error refreshing app");
            ActionContext.clearCurrent();
            return;
        }
        NetworkJob job = new NetworkJob() {
            @Override
            public void run() {
                String key = S.concat(MetricInfo.HTTP_HANDLER, ":", requestHandler.toString());
                Timer timer = metric.startTimer(key);
                ctx.saveLocal();
                try {
                    requestHandler.handle(ctx);
                } catch (Result r) {
                    if (isError(r)) {
                        ctx.handler(FastRequestHandler.DUMB);
                    }
                    try {
                        r = RequestHandlerProxy.GLOBAL_AFTER_INTERCEPTOR.apply(r, ctx);
                    } catch (Exception e) {
                        logger.error(e, "Error calling global after interceptor");
                        r = ActErrorResult.of(e);
                    }
                    if (null == ctx.handler() || isError(r)) {
                        ctx.handler(FastRequestHandler.DUMB);
                    }

                    H.Format fmt = req.accept();
                    if (H.Format.UNKNOWN == fmt) {
                        fmt = req.contentType();
                    }

                    ctx.resp().addHeaderIfNotAdded(H.Header.Names.CONTENT_TYPE, fmt.contentType());
                    r.apply(req, ctx.resp());
                } catch (Exception e) {
                    handleException(e, ctx, "Error handling network request");
                } finally {
                    // we don't destroy ctx here in case it's been passed to
                    // another thread
                    ActionContext.clearCurrent();
                    if (null != timer) {
                        timer.stop();
                    }
                }
            }
        };
        if (method.unsafe() || !requestHandler.express(ctx)) {
            dispatcher.dispatch(job);
        } else {
            job.run();
        }
    }

    private boolean isError(Result r) {
        return r instanceof ErrorResult;
    }

    private void handleException(Exception exception, final ActionContext ctx, String errorMessage) {
        logger.error(exception, errorMessage);
        Result r;
        try {
            r = RequestHandlerProxy.GLOBAL_EXCEPTION_INTERCEPTOR.apply(exception, ctx);
        } catch (Exception e) {
            logger.error(e, "Error calling global exception interceptor");
            r = ActErrorResult.of(e);
        }
        if (null == r) {
            r = ActErrorResult.of(exception);
        } else if (r instanceof ErrorResult) {
            r = ActErrorResult.of(r);
        }
        if (null == ctx.handler()) {
            ctx.handler(FastRequestHandler.DUMB);
        }
        r.apply(ctx.req(), ctx.resp());
    }

    @Override
    public String toString() {
        return app().name();
    }

    private Router router() {
        return app.router(port);
    }

    private static $.Func2<H.Request, String, String> DUMB_CONTENT_SUFFIX_SENSOR = new $.Func2<H.Request, String, String>() {
        @Override
        public String apply(H.Request request, String s) throws NotAppliedException, Osgl.Break {
            return s;
        }
    };

    static class UrlContextProcessor implements $.Func2<H.Request, String, String> {

        private String context;
        private int contextLen;

        UrlContextProcessor(String context) {
            this.context = context;
            this.contextLen = context.length();
        }

        @Override
        public String apply(H.Request request, String s) throws NotAppliedException, Osgl.Break {
            if (s.length() < contextLen || !s.startsWith(context)) {
                throw NotFound.get();
            }
            return s.substring(contextLen, s.length());
        }
    }

    /**
     * Process URL suffix based on suffix
     */
    static class ContentSuffixSensor implements $.Func2<H.Request, String, String> {

        private static final char[] mp3 = {'m', 'p'};
        private static final char[] mp4 = {'m', 'p'};

        private static final char[] mpa = {'m', 'p'};

        private static final char[] pdf = {'p'};
        private static final char[] gif = {};
        private static final char[] tif = {};

        private static final char[] png = {'p'};
        private static final char[] jpg = {};
        private static final char[] mpg = {};
        private static final char[] svg = {'s'};

        private static final char[] avi = {'a', 'v'};

        private static final char[] xml = {'x', 'm'};

        private static final char[] json = {'j', 's', 'o'};

        private static final char[] ico = {'i', 'c'};

        private static final char[] bmp = {'b', 'm'};

        private static final char[] xls = {'x', 'l'};

        private static final char[] wav = {'w'};
        private static final char[] flv = {'f'};
        private static final char[] csv = {'c'};
        private static final char[] mov = {'m'};

        private static final char[] xlsx = {'x', 'l', 's'};

        @Override
        public String apply(H.Request req, String url) throws NotAppliedException, Osgl.Break {
            $.Var<H.Format> fmtBag = $.var();
            String processedUrl = process(url, fmtBag);
            H.Format fmt = fmtBag.get();
            if (null != fmt) {
                req.accept(fmt);
            }
            return processedUrl;
        }

        static String process(String url, $.Var<H.Format> fmtBag) {
            int sz = url.length();
            if (sz < 4) {
                return url;
            }
            int start = sz - 1;
            char c = url.charAt(start);
            int initPos = 1;
            char[] trait;
            int sepPos = 3;
            H.Format fmt = H.Format.JSON;
            switch (c) {
                case '3':
                    trait = mp3;
                    break;
                case '4':
                    trait = mp4;
                    break;
                case 'a':
                    trait = mpa;
                    break;
                case 'f':
                    c = url.charAt(start - 1);
                    initPos = 2;
                    switch (c) {
                        case 'd':
                            trait = pdf;
                            fmt = H.Format.PDF;
                            break;
                        case 'i':
                            c = url.charAt(start - 2);
                            initPos = 3;
                            switch (c) {
                                case 'g':
                                    trait = gif;
                                    fmt = H.Format.GIF;
                                    break;
                                case 't':
                                    trait = tif;
                                    fmt = H.Format.TIF;
                                    break;
                                default:
                                    return url;
                            }
                            break;
                        default:
                            return url;
                    }
                    break;
                case 'g':
                    c = url.charAt(start - 1);
                    initPos = 2;
                    switch (c) {
                        case 'n':
                            trait = png;
                            fmt = H.Format.PNG;
                            break;
                        case 'p':
                            c = url.charAt(start - 2);
                            initPos = 3;
                            switch (c) {
                                case 'j':
                                    trait = jpg;
                                    fmt = H.Format.JPG;
                                    break;
                                case 'm':
                                    trait = mpg;
                                    fmt = H.Format.MPG;
                                    break;
                                default:
                                    return url;
                            }
                            break;
                        case 'v':
                            trait = svg;
                            fmt = H.Format.SVG;
                            break;
                        default:
                            return url;
                    }
                    break;
                case 'i':
                    trait = avi;
                    fmt = H.Format.AVI;
                    break;
                case 'l':
                    trait = xml;
                    fmt = H.Format.XML;
                    break;
                case 'n':
                    sepPos = 4;
                    trait = json;
                    break;
                case 'o':
                    trait = ico;
                    fmt = H.Format.ICO;
                    break;
                case 'p':
                    trait = bmp;
                    fmt = H.Format.BMP;
                    break;
                case 's':
                    trait = xls;
                    fmt = H.Format.XLS;
                    break;
                case 'v':
                    c = url.charAt(start - 1);
                    initPos = 2;
                    switch (c) {
                        case 'a':
                            trait = wav;
                            fmt = H.Format.WAV;
                            break;
                        case 'l':
                            trait = flv;
                            fmt = H.Format.FLV;
                            break;
                        case 's':
                            trait = csv;
                            fmt = H.Format.CSV;
                            break;
                        case 'o':
                            trait = mov;
                            fmt = H.Format.MOV;
                            break;
                        default:
                            return url;
                    }
                    break;
                case 'x':
                    sepPos = 4;
                    trait = xlsx;
                    fmt = H.Format.XLSX;
                    break;
                default:
                    return url;
            }
            char sep = url.charAt(start - sepPos);
            if (sep != '/') {
                return url;
            }
            for (int i = initPos; i < sepPos; ++i) {
                if (url.charAt(start - i) != trait[sepPos - i - 1]) {
                    return url;
                }
            }
            fmtBag.set(fmt);
            return url.substring(0, sz - sepPos - 1);
        }
    }
}
