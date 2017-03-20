package act.util;

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

import act.cli.Command;
import act.cli.Required;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.logging.Logger.Level;
import org.osgl.util.S;

public class LogAdmin {

    @Command(name = "act.log.level.show", help = "Show log level")
    public String showLogLevel(@Required("specify LOGGER name") String name) {
        Logger logger = LogManager.get(name);
        if (logger.isTraceEnabled()) {
            return "trace";
        } else if (logger.isDebugEnabled()) {
            return "debug";
        } else if (logger.isInfoEnabled()) {
            return "info";
        } else if (logger.isWarnEnabled()) {
            return "warn";
        } else if (logger.isErrorEnabled()) {
            return "error";
        }
        return "fatal";
    }

    @Command(
            name = "act.log.level.update",
            help = "Update LOGGER level. Valid levels are:\n\t" +
                    "5 - fatal\n\t" +
                    "4 - error\n\t" +
                    "3 - warn\n\t" +
                    "2 - info\n\t" +
                    "1 - debug\n\t" +
                    "0 - trace"
    )
    public String setLogLevel(
            @Required("specify LOGGER name") String name,
            @Required("specify log level")  int level
    ) {
        Level lvl = convert(level);
        Logger logger = LogManager.get(name);
        logger.setLevel(lvl);
        return S.fmt("LOGGER[%s] level set to %s", name, lvl.toString());
    }

    private Level convert(int level) {
        switch (level) {
            case 0:
                return Level.TRACE;
            case 1:
                return Level.DEBUG;
            case 2:
                return Level.INFO;
            case 3:
                return Level.WARN;
            case 4:
                return Level.ERROR;
            default:
                return Level.FATAL;
        }
    }

}
