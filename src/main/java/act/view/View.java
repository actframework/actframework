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
import act.app.App;
import act.app.event.SysEventId;
import act.plugin.AppServicePlugin;
import act.util.ActContext;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.List;

/**
 * The base class that different View solution should extends
 */
public abstract class View extends AppServicePlugin {

    /**
     * Returns the View solution's name. Recommended name should
     * be in lower case characters. E.g. freemarker, velocity,
     * rythm etc
     */
    public abstract String name();

    @Override
    protected void applyTo(final App app) {
        Act.viewManager().register(this);
    }

    public boolean appliedTo(ActContext context) {
        H.Format format = context.accept();
        return format.isText() || H.Format.UNKNOWN == format;
    }

    /**
     * Sub class must implement this method to load the template
     *
     * @param resourcePath the path to the template
     * @return the template instance or {@code null} if template not found
     */
    // TODO it shall not need context to load template, revise this interface to remove it
    protected abstract Template loadTemplate(String resourcePath);

    /**
     * Sub class must implement this method to load the template from string literal
     *
     * @param content the template content string literal
     * @return the template instance
     * @since 1.6
     */
    protected abstract Template loadInlineTemplate(String content);

    /**
     * Sub class could use this method initialize the implementation
     */
    protected void init(App app) {
    }

    protected void reload(App app) {
        init(app);
    }

    protected final String templateHome() {
        String templateHome = Act.appConfig().templateHome();
        if (S.blank(templateHome) || "default".equals(templateHome)) {
            templateHome = "/" + name();
        }
        return templateHome;
    }

    public DirectRender directRenderFor(H.Format acceptType) {
        return null;
    }

    /**
     * Load template content.
     * <p>
     * This method is used by error reporting feature when app running in dev mode
     *
     * @param template the template path
     * @return the template content in lines
     */
    public List<String> loadContent(String template) {
        File file = new File(templateRootDir(), template);
        if (file.exists() && file.canRead()) {
            return IO.readLines(file);
        }
        return C.list();
    }


    protected final File templateRootDir() {
        App app = Act.app();
        return new File(app.layout().resource(app.base()), templateHome());
    }
}
