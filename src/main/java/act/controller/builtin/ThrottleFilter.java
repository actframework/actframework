package act.controller.builtin;

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
import act.app.event.SysEventId;
import org.osgl.cache.CacheService;
import org.osgl.http.H;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.result.TooManyRequests;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.inject.Singleton;

@Singleton
public class ThrottleFilter {

    public static final String CACHE_NAME = "act.throttle";

    private CacheService cache;

    @Configuration("act.req.throttle")
    private int throttle;

    @Configuration("req.throttle.expire.scale.enabled")
    private boolean expireScale;

    public ThrottleFilter() {
        final App app = Act.app();
        app.jobManager().on(SysEventId.CLASS_LOADER_INITIALIZED, "ThrottleFilter:initCache", new Runnable() {
            @Override
            public void run() {
                cache = app.cache(CACHE_NAME);
            }
        }, true);
    }

    public ThrottleFilter(int throttle, boolean expireScale) {
        E.illegalArgumentIf(throttle < 1);
        this.throttle = throttle;
        this.expireScale = expireScale;
        final App app = Act.app();
        app.jobManager().on(SysEventId.CLASS_LOADER_INITIALIZED, "ThrottleFilter:initCache", new Runnable() {
            @Override
            public void run() {
                cache = app.cache(CACHE_NAME);
            }
        }, true);
    }

    public Result handle(ActionContext actionContext) {
        String key = cacheKey(actionContext);
        if (!expireScale) {
            if (throttle <= cache.incr(key, 1)) {
                return TooManyRequests.get();
            }
        } else {
            Integer curReqCnt = cache.get(key);
            if (null == curReqCnt) {
                curReqCnt = 0;
            }
            int timeout = curReqCnt + 1;
            cache.incr(key, timeout);
            if (curReqCnt >= throttle) {
                return TooManyRequests.get();
            }
        }
        return null;
    }

    private String cacheKey(ActionContext context) {
        H.Request req = context.req();
        return S.concat(req.method(), req.url(), req.ip());
    }
}
