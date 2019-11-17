package act.handler.builtin.controller;

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
import act.handler.ExpressHandler;
import act.view.HotReloading;
import org.osgl.http.H;
import org.osgl.mvc.result.Conflict;

import java.util.concurrent.atomic.AtomicInteger;

public class HotReloadingHandler extends FastRequestHandler implements ExpressHandler, App.HotReloadListener {

    private static final AtomicInteger HIT_COUNTER = new AtomicInteger(0);

    public static HotReloadingHandler INSTANCE = new HotReloadingHandler();

    private static final HotReloading HOT_RELOADING = new HotReloading();

    private HotReloadingHandler() {
        Act.app().registerHotReloadListener(this);
    }

    @Override
    public void preHotReload() {
        HIT_COUNTER.set(1);
    }

    @Override
    public void handle(ActionContext context) {
        // session is not initialized at the moment
        // we have to use other mechanism
        //Integer delay = context.session().cached(DELAY);
        int delay = HIT_COUNTER.get();
        if (delay < 5) {
            delay++;
            HIT_COUNTER.set(delay);
        }
        context.renderArg("delay", delay);
        context.accept(H.Format.HTML);
        HOT_RELOADING.apply(context.req(), context.prepareRespForResultEvaluation());
    }

    @Override
    public String toString() {
        return "error: hot-reloading";
    }
}
