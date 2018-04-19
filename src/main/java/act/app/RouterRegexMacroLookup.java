package act.app;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.conf.AppConfig;
import act.conf.Config;
import act.inject.util.ConfigResourceLoader;
import act.util.LogSupport;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// see https://github.com/actframework/actframework/issues/438

/**
 * This class load router macro definitions and handle the call to expand a
 * router macro.
 */
public class RouterRegexMacroLookup extends LogSupport {

    private static final String MACRO_DEF_FILE = "act.router.macro";

    private Map<String, String> macros = new HashMap<>();

    /**
     * Construct a macro with {@link AppConfig} instance.
     *
     * This will load macro definition from app config. Macro
     * definition in app config shall has key starts with `router.macro`.
     * For example
     *
     * ```
     * router.regex.macro.__user__=[a-z]([a-z_0-9)*
     * ```
     *
     * This will also load from macro definition file named `act.router.macro`
     * after loaded from app config. The macro definition provided in the
     * definition file shall not be prefixed with `act.router.macro`, for example
     *
     * ```
     * __user__=[a-z]([a-z_0-9)*
     * ```
     *
     * The macro definition file name can also be specified using `router.macro_file`
     * configuration.
     *
     * @param config the app config
     */
    public RouterRegexMacroLookup(AppConfig<?> config) {
        final String namespace = "router.macro";
        final String prefix = namespace + ".";
        Map<String, Object> appConfigMacros = config.subSet(namespace);
        for (Map.Entry<String, Object> entry : appConfigMacros.entrySet()) {
            String key = S.afterFirst(entry.getKey(), prefix);
            this.macros.put(key, entry.getValue().toString());
        }
        String macroDefinitionFile = config.get("router.macro_file");
        if (null == macroDefinitionFile) {
            macroDefinitionFile = MACRO_DEF_FILE;
        }
        InputStream is = ConfigResourceLoader.load(macroDefinitionFile, InputStream.class, true);
        if (null != is) {
            Properties properties = IO.loadProperties(is);
            for (Map.Entry entry : properties.entrySet()) {
                this.macros.put(Config.canonical(S.string(entry.getKey())), S.string(entry.getValue()));
            }
        }
    }

    /**
     * Expand a macro.
     *
     * This will look up the macro definition from {@link #macros} map.
     * If not found then return passed in `macro` itself, otherwise return
     * the macro definition found.
     *
     * **note** if macro definition is not found and the string
     * {@link #isMacro(String) comply to macro name convention}, then a
     * warn level message will be logged.
     *
     * @param macro the macro name
     * @return macro definition or macro itself if no definition found.
     */
    public String expand(String macro) {
        if (!isMacro(macro)) {
            return macro;
        }
        String definition = macros.get(Config.canonical(macro));
        if (null == definition) {
            warn("possible missing definition of macro[%s]", macro);
        }
        return null == definition ? macro : definition;
    }

    /**
     * Check if a string is a macro name
     *
     * At the moment we suppose any string starts with `__`
     * (2 underscores) is a macro definition
     *
     * @param s a string to be tested
     * @return `true` if `s` looks like a macro
     */
    private boolean isMacro(String s) {
        return s.startsWith("__");
    }
}
