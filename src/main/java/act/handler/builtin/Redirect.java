package act.handler.builtin;

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

import act.app.ActionContext;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;

public class Redirect extends FastRequestHandler implements ExpressHandler {

    private String url;

    public Redirect(String url) {
        this.url = url;
    }

    @Override
    public void handle(ActionContext context) {
        H.Response resp = context.prepareRespForResultEvaluation();
        if (context.isAjax()) {
            resp.status(H.Status.FOUND_AJAX);
        } else {
            resp.status(H.Status.MOVED_PERMANENTLY);
        }
        resp.header(H.Header.Names.LOCATION, url);
    }

    @Override
    public String toString() {
        return "redirect: " + url;
    }
}
