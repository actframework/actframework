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
import act.util.ActContext;
import act.util.JsonUtilConfig;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.http.Http;
import org.osgl.mvc.result.RenderContent;
import org.osgl.mvc.result.RenderJSON;
import org.osgl.util.Output;

import java.io.Writer;

/**
 * An enhanced version of {@link org.osgl.mvc.result.RenderJSON} that
 * allows {@link act.util.PropertySpec} to be applied to control the
 * output fields
 */
public class FilteredRenderJSON extends RenderJSON {

    public static final FilteredRenderJSON _INSTANCE = new FilteredRenderJSON() {
        @Override
        public String content() {
            return payload().message;
        }

        @Override
        public $.Visitor<Writer> contentWriter() {
            return payload().contentWriter;
        }

        @Override
        public Http.Status status() {
            Http.Status status = payload().status;
            return null == status ? super.status() : status;
        }

        @Override
        public long timestamp() {
            return payload().timestamp;
        }

        @Override
        public boolean isOutputEncoding() {
            return payload().outputEncoding();
        }

        @Override
        public RenderContent setOutputEncoding(boolean outputEncoding) {
            payload().outputEncoding(outputEncoding);
            return this;
        }

        @Override
        public H.Format format() {
            return H.Format.JSON;
        }
    };

    private FilteredRenderJSON() {
        super(H.Format.JSON);
    }

    public FilteredRenderJSON(final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        super(new JsonUtilConfig.JsonWriter(v, spec, false, context));
    }

    public FilteredRenderJSON(H.Status status, final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        super(status, new JsonUtilConfig.JsonWriter(v, spec, false, context));
    }

    public static FilteredRenderJSON of(final Object v, final PropertySpec.MetaInfo spec, final ActionContext context) {
        if (v instanceof String) {
            touchPayload().message((String) v);
        } else if (v instanceof $.Visitor) {
            touchPayload().contentWriter(($.Visitor) v);
        } else {
            if (context.isNonBlock()) {
                touchPayload().stringContentProducer(new JsonUtilConfig.JsonWriter(v, spec, false, context).asContentProducer());
            } else {
                touchPayload().contentWriter(new JsonUtilConfig.JsonWriter(v, spec, false, context));
            }
        }
        return _INSTANCE;
    }

    public static FilteredRenderJSON of(H.Status status, final Object v, final PropertySpec.MetaInfo spec, final ActionContext context) {
        touchPayload().status(status);
        return of(v, spec, context);
    }
}
