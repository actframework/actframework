package act.controller.builtin;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2020 ActFramework
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

import act.annotations.MultiLines;
import act.app.App;
import act.cli.*;
import act.conf.AppConfig;
import act.controller.annotation.Port;
import act.controller.annotation.UrlContext;
import act.handler.CliHandler;
import act.handler.builtin.cli.CliHandlerProxy;
import act.util.Stateless;
import org.osgl.$;
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static act.controller.Controller.Util.*;

/**
 * Display CLI help page
 */
@UrlContext("cmd")
@Stateless
@Port({AppConfig.PORT_CLI_OVER_HTTP, "default"})
public class HelpPage {

    @Inject
    private CliDispatcher dispatcher;

    @Before
    public void ensureNonDevMode(App app) {
        notFoundIf(app.isProd());
    }

    @GetAction
    public void index() {
        SortedSet<CliCmdInfo> sysCommands = dispatcher.commandInfoList(true, false);
        SortedSet<CliCmdInfo> appCommands = dispatcher.commandInfoList(false, true);
        render("~cmd_index.html", sysCommands, appCommands);
    }

    @GetAction("{cmdName}")
    public void help(String cmdName) {
        CliHandler handler = dispatcher.handler(cmdName);
        notFoundIfNull(handler);
        CliCmdInfo cmd = getCmdInfo(handler);
        notFoundIfNull(cmd);
        cmd.name = Keyword.of(cmdName).kebabCase();
        render("~cmd_help.html", cmd);
    }

    private CliCmdInfo getCmdInfo(CliHandler handler) {
        if (handler instanceof CliHandlerProxy) {
            return getCmdInfo((CliHandlerProxy) handler);
        } else {
            return null;
        }
    }

    private CliCmdInfo getCmdInfo(CliHandlerProxy proxy) {
        CliCmdInfo info = new CliCmdInfo();
        info.help = proxy.commandLine().right();
        info.multiLinesParams = new HashSet<>();
        info.params = getCmdParamInfo(proxy, info.multiLinesParams);
        return info;
    }

    private Map<String, String> getCmdParamInfo(CliHandlerProxy proxy, Set<String> multiLinesParams) {
        CommandExecutor executor = proxy.executor();
        Class<?> host = $.getProperty(executor, "commanderClass");
        Method method = $.getProperty(executor, "method");
        Map<String, String> map = new TreeMap<>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] paramAnns = method.getParameterAnnotations();
        for (int i = 0; i < paramTypes.length; ++i) {
            Class<?> pt = paramTypes[i];
            Named named = _getAnno(paramAnns, i, Named.class);
            E.unexpectedIf(null == named, "Cannot find name of the param: %s.%s(%s|%s)", host.getSimpleName(), method.getName(), i, pt.getSimpleName());
            String pn = named.value();
            if (null != _getAnno(paramAnns, i, MultiLines.class)) {
                multiLinesParams.add(pn);
            }
            Required required = _getAnno(paramAnns, i, Required.class);
            if (null != required) {
                map.put(pn, helpOf(required));
            } else {
                act.cli.Optional optional = _getAnno(paramAnns, i, act.cli.Optional.class);
                if (null != optional) {
                    String help = helpOf(optional);
                    map.put(pn, help);
                } else {
                    map.put(pn, "no help");
                }
            }
        }
        return map;
    }

    private static String helpOf(Required required) {
        String help = required.help();
        return S.blank(help) ? required.value() : help;
    }

    private static String helpOf(act.cli.Optional optional) {
        String help = optional.help();
        return S.blank(help) ? optional.value() : help;
    }

    private <T extends Annotation> T _getAnno(Annotation[][] paramAnnotations, int paramIndex, Class<T> annoType) {
        Annotation[] pas = paramAnnotations[paramIndex];
        for (int j = 0; j < pas.length; ++j) {
            Annotation pa = pas[j];
            if (annoType.isAssignableFrom(pa.annotationType())) {
                return (T) pa;
            }
        }
        return null;
    }

}
