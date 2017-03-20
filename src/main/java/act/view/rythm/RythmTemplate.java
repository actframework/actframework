package act.view.rythm;

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
import act.mail.MailerContext;
import act.view.TemplateBase;
import org.osgl.util.E;
import org.rythmengine.RythmEngine;
import org.rythmengine.resource.ITemplateResource;
import org.rythmengine.template.ITemplate;

import java.util.Locale;
import java.util.Map;

public class RythmTemplate extends TemplateBase {

    private RythmEngine engine;
    private String path;

    public RythmTemplate(RythmEngine engine, String path) {
        E.NPE(engine);
        this.engine = engine;
        this.path = path;
    }

    @Override
    protected void beforeRender(ActionContext context) {
        Locale locale = context.locale();
        engine = engine.prepare(locale);
    }

    @Override
    protected void beforeRender(MailerContext context) {
        Locale locale = context.locale();
        engine = engine.prepare(locale);
    }

    @Override
    protected String render(Map<String, Object> renderArgs) {
        // TODO handle render exception
        ITemplate t = template(renderArgs);
        return t.render();
    }

    private org.rythmengine.template.ITemplate template(Map<String, Object> renderArgs) {
        return engine.getTemplate(path, renderArgs);
    }

    public static RythmTemplate find(RythmEngine engine, String path) {
        ITemplateResource resource = engine.resourceManager().getResource(path);
        if (!resource.isValid()) {
            return null;
        } else {
            return new RythmTemplate(engine, path);
        }
    }
}
