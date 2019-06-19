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
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;
import org.osgl.util.S;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Render object as CSV
 */
public class RenderCSV extends RenderContent {

    private static RenderCSV _INSTANCE = new RenderCSV() {
        @Override
        public String content() {
            Payload payload = payload();
            return null == payload.stringContentProducer ? payload.message : payload.stringContentProducer.apply();
        }

        @Override
        public $.Visitor<Writer> contentWriter() {
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
        super(new $.Visitor<Writer>() {
            @Override
            public void visit(Writer writer) throws $.Break {
                render(writer, v, spec, context);
            }
        }, H.Format.CSV);
    }

    public RenderCSV(H.Status status, final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        super(status, new $.Visitor<Writer>() {
            @Override
            public void visit(Writer writer) throws $.Break {
                render(writer, v, spec, context);
            }
        }, H.Format.CSV);
    }

    public static RenderCSV of(final Object v, final PropertySpec.MetaInfo spec, final ActContext context) {
        touchPayload().contentWriter(new $.Visitor<Writer>() {
            @Override
            public void visit(Writer writer) throws $.Break {
                render(writer, v, spec, context);
            }
        });
        return _INSTANCE;
    }

    public static RenderCSV of(H.Status status, final Object v, final PropertySpec.MetaInfo spec, final ActionContext context) {
        Payload payload = touchPayload().status(status);
        if (context.isLargeResponse()) {
            payload.contentWriter(new $.Visitor<Writer>() {
                @Override
                public void visit(Writer writer) throws $.Break {
                    render(writer, v, spec, context);
                }
            });
        } else {
            payload.stringContentProducer(new $.Func0<String>() {
                @Override
                public String apply() throws NotAppliedException, $.Break {
                    StringWriter writer = new StringWriter();
                    render(writer, v, spec, context);
                    return writer.toString();
                }
            });
        }
        return _INSTANCE;
    }

    private static void render(Writer writer, Object v, PropertySpec.MetaInfo spec, ActContext context) {
        setDownloadHeader(context);
        CliView.CSV.render(writer, v, spec, context);
    }

    private static void setDownloadHeader(ActContext context) {
        if (context instanceof ActionContext) {
            ActionContext ctx = (ActionContext) context;
            ctx.resp().contentDisposition(ctx.attachmentName(), false);
        }
    }
}
