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

import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;

/**
 * Provides logging support utility methods to extended classes.
 */
public class LogSupport {

    protected final Logger logger = LogManager.get(getClass());

    protected void trace(String format, Object ... args) {
        logger.trace(format, args);
    }

    protected boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    protected void trace(Throwable t, String format, Object ... args) {
        logger.trace(t, format, args);
    }

    protected boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    protected void debug(String format, Object ... args) {
        logger.debug(format, args);
    }

    protected void debug(Throwable t, String format, Object ... args) {
        logger.debug(t, format, args);
    }

    protected void info(String format, Object ... args) {
        logger.info(format, args);
    }

    protected void info(Throwable t, String format, Object ... args) {
        logger.info(t, format, args);
    }

    protected void warn(String format, Object ... args) {
        logger.warn(format, args);
    }

    protected void warn(Throwable t, String format, Object ... args) {
        logger.warn(t, format, args);
    }

    protected void error(String format, Object ... args) {
        logger.error(format, args);
    }

    protected void error(Throwable t, String format, Object ... args) {
        logger.error(t, format, args);
    }

    protected void fatal(String format, Object ... args) {
        logger.fatal(format, args);
    }

    public void fatal(Throwable t, String format, Object ... args) {
        logger.fatal(t, format, args);
    }

}
