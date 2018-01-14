package act.cli;

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
import jline.console.ConsoleReader;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Cli over http context
 */
public class CliOverHttpContext extends CliContext {

    public CliOverHttpContext(ActionContext actionContext, OutputStream os) {
        super(line(actionContext), actionContext.app(), console(actionContext, os), session(actionContext), false);
    }

    private static String line(ActionContext actionContext) {
        String cmd = null;
        S.Buffer sb = S.buffer();
        for (String s : actionContext.paramKeys()) {
            if ("cmd".equals(s)) {
                cmd = actionContext.paramVal(s);
            } else if (s.startsWith("-")) {
                String val = actionContext.paramVal(s);
                if (S.notBlank(val)) {
                    val = val.replaceAll("[\n\r]+", "<br/>").trim();
                    if (val.contains(" ")) {
                        char quote = val.contains("\"") ? '\'' : '\\';
                        val = S.wrap(val, quote);
                    }
                    if (s.contains(",")) {
                        s = S.before(s, ",");
                    }
                    sb.append(s).append(" ").append(val).append(" ");
                }
            }
        }
        E.illegalArgumentIf(null == cmd, "cmd param required");
        return S.builder(cmd).append(" ").append(sb.toString()).toString();
    }

    private static ConsoleReader console(ActionContext actionContext, OutputStream os) {
        try {
            return new CliOverHttpConsole(actionContext, os);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    private static CliSession session(ActionContext actionContext) {
        return new CliOverHttpSession(actionContext);
    }
}
