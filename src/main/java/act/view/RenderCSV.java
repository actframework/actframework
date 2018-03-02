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
import act.route.UrlPath;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;
import org.osgl.util.Output;
import org.osgl.util.S;

/**
 * Render object as CSV
 */
public class RenderCSV extends RenderContent {

    private static RenderCSV _INSTANCE = new RenderCSV() {
        @Override
        public String content() {
            return payload().message;
        }

        @Override
        public $.Visitor<Output> contentWriter() {
            return payload().contentWriter;
        }

        @Override
        public long timestamp() {
            return payload().timestamp;
        }
    };

    private RenderCSV() {
        super(H.Format.CSV);
    }

    public RenderCSV(final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        super(new $.Visitor<Output>() {
            @Override
            public void visit(Output output) throws Osgl.Break {
                render(output, v, spec, context);
            }
        }, H.Format.CSV);
    }

    public RenderCSV(H.Status status, final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        super(status, new $.Visitor<Output>() {
            @Override
            public void visit(Output output) throws Osgl.Break {
                render(output, v, spec, context);
            }
        }, H.Format.CSV);
    }

    public static RenderCSV get(final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        touchPayload().contentWriter(new $.Visitor<Output>() {
            @Override
            public void visit(Output output) throws Osgl.Break {
                render(output, v, spec, context);
            }
        });
        return _INSTANCE;
    }

    public static RenderCSV get(H.Status status, final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        touchPayload().status(status).contentWriter(new $.Visitor<Output>() {
            @Override
            public void visit(Output output) throws Osgl.Break {
                render(output, v, spec, context);
            }
        });
        return _INSTANCE;
    }

    private static void render(Output output, Object v, PropertySpec.MetaInfo spec, ActContext context) {
        setDownloadHeader(context);
        CliView.CSV.render(output, v, spec, context);
    }

    private static void setDownloadHeader(ActContext context) {
        if (context instanceof ActionContext) {
            ActionContext ctx = (ActionContext) context;
            UrlPath path = ctx.urlPath();
            String fileName = S.concat(S.underscore(path.lastPart()), ".csv");
            ctx.resp().contentDisposition(fileName, false);
        }
    }
}
