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

import static org.osgl.http.H.Format.*;

import act.ActResponse;
import act.app.ActionContext;
import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.result.*;
import org.osgl.storage.ISObject;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * Render a template with template path and arguments provided
 * by {@link ActionContext}
 */
public class RenderAny extends Result {

    public static final RenderAny INSTANCE = new RenderAny();

    private boolean ignoreMissingTemplate = false;

    public RenderAny() {
        super(H.Status.OK);
    }

    public RenderAny ignoreMissingTemplate() {
        this.ignoreMissingTemplate = true;
        return this;
    }

    @Override
    public void apply(H.Request req, H.Response resp) {
        throw E.unsupport("RenderAny does not support " +
                "apply to request and response. Please use apply(AppContext) instead");
    }

    // TODO: Allow plugin to support rendering pdf, xls or other binary types
    public void apply(ActionContext context) {
        Boolean hasTemplate = context.hasTemplate();
        if (null != hasTemplate && hasTemplate) {
            RenderTemplate.get(context.successStatus()).apply(context);
            return;
        }
        H.Format fmt = context.accept();
        if (fmt.isSameTypeWith(UNKNOWN)) {
            // let's guess the request needs HTML
            fmt = HTML;
        }
        Result result = null;
        if (JSON.isSameTypeWith(fmt)) {
            List<String> varNames = context.__appRenderArgNames();
            Map<String, Object> map = new HashMap<>(context.renderArgs());
            if (null != varNames && !varNames.isEmpty()) {
                for (String name : varNames) {
                    map.put(name, context.renderArg(name));
                }
            }
            result = new RenderJSON(map);
        } else if (XML.isSameTypeWith(fmt)) {
            List<String> varNames = context.__appRenderArgNames();
            Map<String, Object> map = new HashMap<>();
            if (null != varNames && !varNames.isEmpty()) {
                for (String name : varNames) {
                    map.put(name, context.renderArg(name));
                }
            }
            result = new FilteredRenderXML(map, null, context);
        } else if (fmt.isSameTypeWithAny(HTML, TXT, CSV)) {
            if (!ignoreMissingTemplate) {
                throw E.unsupport("Template[%s] not found", context.templatePath());
            }
            context.nullValueResultIgnoreRenderArgs().apply(context.req(), context.prepareRespForResultEvaluation());
            return;
        } else if (fmt.isSameTypeWithAny(PDF, XLS, XLSX, DOC, DOCX)) {
            List<String> varNames = context.__appRenderArgNames();
            if (null != varNames && !varNames.isEmpty()) {
                Object firstVar = context.renderArg(varNames.get(0));
                String action = S.str(context.actionPath()).afterLast(".").toString();
                if (firstVar instanceof File) {
                    File file = (File) firstVar;
                    result = new RenderBinary(file, action);
                } else if (firstVar instanceof InputStream) {
                    InputStream is = (InputStream) firstVar;
                    result = new RenderBinary(is, action);
                } else if (firstVar instanceof ISObject) {
                    ISObject sobj = (ISObject) firstVar;
                    result = new RenderBinary(sobj.asInputStream(), action);
                }
                if (null == result) {
                    throw E.unsupport("Unknown render arg type [%s] for binary response", firstVar.getClass());
                }
            } else {
                throw E.unexpected("No render arg found for binary response");
            }
        }
        if (null != result) {
            ActResponse<?> resp = context.prepareRespForResultEvaluation();
            result.status(context.successStatus()).apply(context.req(), resp);
        } else {
            throw E.unexpected("Unknown accept content type: %s", fmt.contentType());
        }
    }

    public static RenderAny get() {
        return INSTANCE;
    }

    public static void clearThreadLocals() {
        RenderTemplate.renderArgsBag.remove();
    }

}
