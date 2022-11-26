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

import static org.rythmengine.conf.RythmConfigurationKey.*;

import act.Act;
import act.app.App;
import act.conf.AppConfig;
import act.view.Template;
import act.view.VarDef;
import act.view.View;
import org.osgl.util.C;
import org.osgl.util.S;
import org.rythmengine.Rythm;
import org.rythmengine.RythmEngine;
import org.rythmengine.extension.IFormatter;
import org.rythmengine.extension.ISourceCodeEnhancer;
import org.rythmengine.extension.Transformer;
import org.rythmengine.resource.ClasspathResourceLoader;
import org.rythmengine.template.ITemplate;
import org.rythmengine.utils.RawData;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implement a view with Rythm Template Engine
 */
public class RythmView extends View {

    public static final String ID = "rythm";

    ConcurrentMap<App, RythmEngine> engines = new ConcurrentHashMap<>();
    ConcurrentMap<String, Template> templates = new ConcurrentHashMap<>();
    ConcurrentMap<String, String> missings = new ConcurrentHashMap<>();

    private boolean isDev;

    public RythmView() {
        isDev = Act.isDev();
    }

    @Override
    public String name() {
        return ID;
    }

    @Override
    protected Template loadTemplate(String resourcePath) {
        App app = Act.app();
        if (isDev) {
            return loadTemplateFromResource(resourcePath, app);
        }
        if (missings.containsKey(resourcePath)) {
            return null;
        }
        Template template = templates.get(resourcePath);
        if (null == template) {
            Template newTemplate = loadTemplateFromResource(resourcePath, app);
            if (null != newTemplate) {
                template = templates.putIfAbsent(resourcePath, newTemplate);
                if (null == template) {
                    template = newTemplate;
                }
            } else {
                missings.put(resourcePath, resourcePath);
            }
        }
        return template;
    }

    @Override
    protected Template loadInlineTemplate(String content) {
        RythmEngine engine = getEngine(Act.app());
        return new RythmTemplate(engine, content, true);
    }

    public RythmEngine getEngine(App app) {
        RythmEngine engine = engines.get(app);
        if (null == engine) {
            RythmEngine newEngine = createEngine(app);
            engine = engines.putIfAbsent(app, newEngine);
            if (null == engine) {
                engine = newEngine;
            } else {
                newEngine.shutdown();
            }
        }
        return engine;
    }

    private Template loadTemplateFromResource(String resourcePath, App app) {
        RythmEngine engine = getEngine(app);
        return RythmTemplate.find(engine, resourcePath);
    }

    private RythmEngine createEngine(App app) {
        AppConfig config = app.config();
        Properties p = new Properties();

        p.put(ENGINE_MODE.getKey(), Act.mode().isDev() ? Rythm.Mode.dev : Rythm.Mode.prod);
        p.put(ENGINE_PLUGIN_VERSION.getKey(), Act.VERSION.getVersion());
        p.put(ENGINE_CLASS_LOADER_PARENT_IMPL.getKey(), app.classLoader());
        p.put(HOME_TMP.getKey(), createTempHome(app));
        p.put(I18N_LOCALE.getKey(), config.locale());
        p.put(RESOURCE_BUNDLE_ENCODING.getKey(), config.resourceBundleEncoding());

        Map map = config.rawConfiguration();
        for (Object k : map.keySet()) {
            String key = k.toString();
            if (key.startsWith("rythm.")) {
                p.put(key, map.get(key));
            }
        }

        String appRestricted = p.getProperty("rythm.sandbox.restricted_classes", "");
        appRestricted += ";act.*";
        p.put(SANDBOX_RESTRICTED_CLASS.getKey(), appRestricted);

        p.put(HOME_TEMPLATE.getKey(), templateRootDir());

        p.put(CODEGEN_SOURCE_CODE_ENHANCER.getKey(), new ISourceCodeEnhancer() {
            @Override
            public List<String> imports() {
                return C.list();
            }

            @Override
            public String sourceCode() {
                return "";
            }

            @Override
            public Map<String, ?> getRenderArgDescriptions() {
                Map<String, String> map = new HashMap<>();
                for (VarDef var : Act.viewManager().implicitActionViewVariables()) {
                    map.put(var.name(), var.type());
                }
                return map;
            }

            @Override
            public void setRenderArgs(ITemplate iTemplate) {
                // no need to set render args here as
                // it's all done at TemplateBase#exposeImplicitVariables
            }
        });

        RythmEngine engine = new RythmEngine(p);
        engine.setDateFormatFactory(new DateFormatFactory(config));
        engine.resourceManager().prependResourceLoader(new ClasspathResourceLoader(engine, ID));
        if (config.defaultView() == this) {
            String home = config.templateHome();
            if (S.neq("default", home) && S.neq(ID, home)) {
                engine.resourceManager().prependResourceLoader(new ClasspathResourceLoader(engine, home));
            }
        }
        engine.registerTransformer("rythm", "", RythmView.class);

        Tags tags = app.getInstance(Tags.class);
        tags.register(engine);

        return engine;
    }

    public void registerTransformer(App app, Class<?> clazz) {
        getEngine(app).registerTransformer(clazz);
    }

    public void registerBuiltInTransformer(App app, Class<?> clazz) {
        getEngine(app).registerTransformer("rythm", "([^a-zA-Z0-9_]s\\(\\)|^s\\(\\))", clazz);
    }

    public void registerFormatter(App app, IFormatter formatter) {
        getEngine(app).extensionManager().registerFormatter(formatter);
    }

    @Override
    protected void reload(App app) {
        engines.remove(app);
        super.reload(app);
    }

    private File createTempHome(App app) {
        String tmp = System.getProperty("java.io.tmpdir");
        File f =  new File(tmp, "__rythm_" + app.name());
        if (!f.exists() && !f.mkdirs()) {
            f = new File(tmp);
        }
        return f;
    }

    @Transformer
    public static RawData errorLine(String data, int errorColumn) {
        String line = S.string(data);
        if (errorColumn > -1 && errorColumn < line.length()) {
            String a = line.substring(0, errorColumn);
            char c = line.charAt(errorColumn);
            String b = line.substring(errorColumn + 1);
            line = S.concat(a, "<span_class='error-column'>", c, "</span>", b);
            line = S.replace(" ").with("&nbsp;").in(line);
            line = S.replace("span_class").with("span class").in(line);
        } else {
            line = S.replace(" ").with("&nbsp;").in(line);
        }
        return new RawData(line);
    }

    @Transformer
    public static RawData errorLine(RawData data, int errorColumn) {
        String line = S.string(data);
        if (errorColumn > -1 && errorColumn < line.length()) {
            String a = line.substring(0, errorColumn);
            char c = line.charAt(errorColumn);
            String b = line.substring(errorColumn + 1);
            line = S.concat(a, "<span_class='error-column'>", c, "</span>", b);
            line = S.replace(" ").with("&nbsp;").in(line);
            line = S.replace("span_class").with("span class").in(line);
        } else {
            line = S.replace(" ").with("&nbsp;").in(line);
        }
        return new RawData(line);
    }

}
