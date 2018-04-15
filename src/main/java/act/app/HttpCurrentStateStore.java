package act.app;

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

import org.osgl.http.CurrentStateStore;
import org.osgl.http.H;

class HttpCurrentStateStore implements CurrentStateStore {
    @Override
    public H.Request request() {
        ActionContext ctx = ActionContext.current();
        return null == ctx ? null : ctx.req();
    }

    @Override
    public H.Response response() {
        ActionContext ctx = ActionContext.current();
        return null == ctx ? null : ctx.resp();
    }

    @Override
    public H.Session session() {
        ActionContext ctx = ActionContext.current();
        return null == ctx ? null : ctx.session();
    }

    @Override
    public H.Flash flash() {
        ActionContext ctx = ActionContext.current();
        return null == ctx ? null : ctx.flash();
    }

    @Override
    public void session(H.Session session) {
    }

    @Override
    public void request(H.Request request) {
    }

    @Override
    public void response(H.Response response) {
    }

    @Override
    public void flash(H.Flash flash) {
    }

    @Override
    public void clear() {
    }
}
