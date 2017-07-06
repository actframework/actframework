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
import act.cli.view.CliView;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;

/**
 * An enhanced version of {@link org.osgl.mvc.result.RenderJSON} that
 * allows {@link act.util.PropertySpec} to be applied to control the
 * output fields
 */
public class FilteredRenderJSON extends RenderContent {

    public static final FilteredRenderJSON _INSTANCE = new FilteredRenderJSON() {
        @Override
        public String content() {
            return payload().message;
        }

        @Override
        public long timestamp() {
            return payload().timestamp;
        }
    };

    private FilteredRenderJSON() {
        super(H.Format.JSON);
    }

    public FilteredRenderJSON(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(render(v, spec, context), H.Format.JSON);
    }

    public static FilteredRenderJSON get(Object v, PropertySpec.MetaInfo spec, ActionContext context) {
        touchPayload().message(render(v, spec, context));
        return _INSTANCE;
    }

    public FilteredRenderJSON(H.Status status, Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(status, render(v, spec, context), H.Format.JSON);
    }

    public static FilteredRenderJSON get(H.Status status, Object v, PropertySpec.MetaInfo spec, ActionContext context) {
        touchPayload().message(render(v, spec, context)).status(status);
        return _INSTANCE;
    }

    private static String render(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        return CliView.JSON.render(v, spec, context);
    }
}
