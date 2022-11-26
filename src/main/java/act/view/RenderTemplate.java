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

import act.Act;
import act.app.ActionContext;
import act.exception.ActException;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;

/**
 * Render a template with template path and arguments provided
 * by {@link ActionContext}
 */
public class RenderTemplate extends RenderAny {

    public static RenderTemplate INSTANCE = new RenderTemplate() {
        @Override
        public H.Status status() {
            H.Status status = payload().status;
            return (null == status) ? super.status() : status;
        }

        @Override
        public long timestamp() {
            return payload().timestamp;
        }
    };

    static final ThreadLocal<Map<String, Object>> renderArgsBag = new ThreadLocal<>();

    private RenderTemplate() {
    }

    @Override
    public void apply(H.Request req, H.Response resp) {
        renderArgsBag.remove();
        throw E.unsupport("RenderTemplate does not support " +
                "apply to request and response. Please use apply(AppContext) instead");
    }

    public void apply(ActionContext context) {
        Map<String, Object> renderArgs = renderArgsBag.get();
        if (null != renderArgs && !renderArgs.isEmpty()) {
            for (String key : renderArgs.keySet()) {
                context.renderArg(key, renderArgs.get(key));
            }
        }
        ViewManager vm = Act.viewManager();
        Template t = vm.load(context);
        if (null == t) {
            throw new ActException("Render template[%s] not found", context.templatePath());
        }
        applyStatus(context.resp());
        H.Request req = context.req();
        H.Response resp = context.prepareRespForResultEvaluation();
        setContentType(req, resp, t);
        applyBeforeCommitHandler(req, resp);
        t.merge(context);
        applyAfterCommitHandler(req, resp);
    }

    protected void setContentType(H.Request req, H.Response resp, Template template) {
        H.Format fmt = req.accept();
        if (fmt == H.Format.UNKNOWN) {
            fmt = H.Format.HTML;
        }
        String s = fmt.contentType();
        String encoding = resp.characterEncoding();
        if(S.notBlank(encoding)) {
            s = S.concat(s, "; charset=", encoding);
        }

        resp.initContentType(s);
    }

    public static RenderTemplate get() {
        return INSTANCE;
    }

    public static RenderTemplate get(H.Status status) {
        touchPayload().status(status);
        return INSTANCE;
    }

    public static RenderTemplate of(Map<String, Object> args) {
        renderArgsBag.set(args);
        return INSTANCE;
    }

    public static RenderTemplate of(H.Status status, Map<String, Object> args) {
        touchPayload().status(status);
        renderArgsBag.set(args);
        return INSTANCE;
    }
}
