package act.handler;

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
import act.handler.builtin.controller.FastRequestHandler;
import act.security.CORS;
import org.osgl.http.H;

public class OptionsRequestHandler extends FastRequestHandler implements ExpressHandler {

    private CORS.Spec corsSpec;

    public OptionsRequestHandler(CORS.Spec corsSpec) {
        this.corsSpec = corsSpec;
    }

    @Override
    public CORS.Spec corsSpec() {
        return this.corsSpec;
    }

    @Override
    public void handle(ActionContext context) {
        context.resp().status(H.Status.NO_CONTENT).writeContent("");
    }

    @Override
    public String toString() {
        return "OPTIONS handler";
    }
}
