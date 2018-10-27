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
import act.ActResponse;
import act.app.ActionContext;
import act.mail.MailerContext;
import org.osgl.http.H;
import org.osgl.util.Charsets;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Base class for {@link Template} implementations
 */
public abstract class TemplateBase implements Template {

    private static final Charset UTF8 = Charsets.UTF_8;

    @Override
    public void merge(ActionContext context) {
        Map<String, Object> renderArgs = context.renderArgs();
        if (!context.isByPassImplicitTemplateVariable()) {
            exposeImplicitVariables(renderArgs, context);
        }
        beforeRender(context);
        ActResponse resp = context.resp();
        merge(renderArgs, resp);
        //resp.commit();
    }

    @Override
    public String render(ActionContext context) {
        Map<String, Object> renderArgs = context.renderArgs();
        exposeImplicitVariables(renderArgs, context);
        beforeRender(context);
        return render(renderArgs);
    }

    @Override
    public String render(MailerContext context) {
        Map<String, Object> renderArgs = context.renderArgs();
        exposeImplicitVariables(renderArgs, context);
        beforeRender(context);
        return render(renderArgs);
    }

    @Override
    public boolean supportCache() {
        return true;
    }

    /**
     * Sub class can implement this method to inject logic that needs to be done
     * before rendering happening
     *
     * @param context
     */
    protected void beforeRender(ActionContext context) {}

    /**
     * Sub class can implement this method to inject logic that needs to be done
     * before rendering happening
     *
     * @param context
     */
    protected void beforeRender(MailerContext context) {}

    protected void merge(Map<String, Object> renderArgs, H.Response response) {
        String result = render(renderArgs);
        response.writeContent(result);
    }

    protected abstract String render(Map<String, Object> renderArgs);

    private void exposeImplicitVariables(Map<String, Object> renderArgs, ActionContext context) {
        for (ActionViewVarDef var : Act.viewManager().implicitActionViewVariables()) {
            Object val = var.eval(context);
            if (null != val) {
                renderArgs.put(var.name(), val);
            }
        }
    }


    private void exposeImplicitVariables(Map<String, Object> renderArgs, MailerContext context) {
        for (MailerViewVarDef var : Act.viewManager().implicitMailerViewVariables()) {
            Object val = var.eval(context);
            if (null != val) {
                renderArgs.put(var.name(), val);
            }
        }
    }
}
