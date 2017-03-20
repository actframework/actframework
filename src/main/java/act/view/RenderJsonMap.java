package act.view;

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
import org.osgl.http.H;

/**
 * Render json Map using all {@link ActionContext#renderArgs}
 * by {@link ActionContext}.
 *
 * Note this will render the JSON result without regarding to
 * the http `Accept` header
 */
public class RenderJsonMap extends RenderAny {

    public static final RenderJsonMap INSTANCE = new RenderJsonMap();

    public RenderJsonMap() {
    }

    public void apply(ActionContext context) {
        context.accept(H.Format.JSON);
        super.apply(context);
    }

    public static RenderJsonMap get() {
        return INSTANCE;
    }
}
