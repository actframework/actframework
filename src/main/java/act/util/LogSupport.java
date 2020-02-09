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

import act.inject.param.NoBind;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

/**
 * Provides logging support utility methods to extended classes.
 */
@Stateless
public class LogSupport {

    public static final String DOUBLE_DASHED_LINE = S.times('=', 80);
    public static final String DASHED_LINE = S.times('-', 80);
    public static final String STAR_LINE = S.times('*', 80);
    public static final String HASH_SYMBOL_LINE = S.times('#', 80);
    public static final String TILD_LINE = S.times('~', 80);

    protected final transient Logger logger;

    public LogSupport() {
        logger = LogManager.get(getClass());
    }

    public final Logger logger() {
        return logger;
    }

    /**
     * Print a line of 80 `=` chars
     */
    protected void printDoubleDashedLine() {
        info(DOUBLE_DASHED_LINE);
    }

    /**
     * Print a line of 80 `-` chars
     */
    protected void printDashedLine() {
        info(DASHED_LINE);
    }

    /**
     * Print a line of 80 `*` chars
     */
    protected void printStarLine() {
        info(STAR_LINE);
    }

    /**
     * Print a line of 80 `#` chars
     */
    protected void printHashSimbolLine() {
        info(HASH_SYMBOL_LINE);
    }

    /**
     * Print a line of 80 `~` chars
     */
    protected void printTildLine() {
        info(TILD_LINE);
    }

    /**
     * Print a blank line
     */
    protected void println() {
        info("");
    }

    /**
     * Print formatted string in the center of 80 chars line, left and right padded.
     *
     * @param format
     *      The string format pattern
     * @param args
     *      The string format arguments
     */
    protected void printCenter(String format, Object... args) {
        String text = S.fmt(format, args);
        info(S.center(text, 80));
    }

    /**
     * Print the lead string followed by centered formatted string. The whole
     * length of the line is 80 chars.
     *
     * Example:
     *
     * ```java
     * printCenterWithLead(" *", "Hello %s", "World");
     * ```
     *
     * will print something like
     *
     * ```
     *  *              Hello World
     * ```
     *
     * Note the above is just a demo, the exact number of whitespace might not be correct.
     *
     *
     * @param lead
     *      the lead string
     * @param format
     *      The string format pattern
     * @param args
     *      The string format arguments
     */
    protected void printCenterWithLead(String lead, String format, Object ... args) {
        String text = S.fmt(format, args);
        int len = 80 - lead.length();
        info(S.concat(lead, S.center(text, len)));
    }

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
