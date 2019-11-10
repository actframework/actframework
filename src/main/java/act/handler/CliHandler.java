package act.handler;

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
import act.Destroyable;
import act.cli.CliContext;
import act.cli.CliSession;
import org.osgl.$;

import java.util.List;

/**
 * Defines a thread-save function object that can be applied
 * to a {@link CliContext} context to
 * produce certain output which could be applied to cli
 * associated with the context
 */
public interface CliHandler extends $.Function<CliContext, Void>, Destroyable {
    /**
     * Invoke handler upon a cli context
     *
     * @param context the cli context
     */
    void handle(CliContext context);

    /**
     * Returns the command line information that is used to display the
     * command in help. For example the {@link act.cli.builtin.Help} command is
     * <p>
     *     <code>help [options] [command] show help message</code>
     * </p>
     * So the method of {@code Help} command should return
     * <p>
     *     <code>$.T2("help [options] [command]", "show help message")</code>
     * </p>
     * @return the command line
     */
    $.T2<String, String> commandLine();

    /**
     * Returns the summary of the command. This is used to display the help information
     * about this command. If <code>null</code> or empty string returned, then there will
     * be no summary section in the help for this command
     * @return the command summary
     */
    String summary();

    /**
     * Returns a list of options information to be displayed in help of
     * this command. E.g. the options of {@link act.cli.builtin.Help} command
     * is
     * <p>
     *     <code>-s --system list system commands</code>
     *     <code>-a --app list app commands</code>
     * </p>
     * Then the method of {@code Help} should return a list of
     * <ul>
     *     <li><code>$.T2("-s --system", "list system commands")</code></li>
     *     <li><code>$.T2("-a --app", "list application commands")</code></li>
     * </ul>
     *
     * @return the options information about this command
     */
    List<$.T2<String, String>> options();


    /**
     * Check if this handler applied in a specific {@link act.Act.Mode}
     * @return {@code true} if this handler applied in the mode, or {@code false} otherwise
     */
    boolean appliedIn(Act.Mode mode);

    /**
     * Reset session cursor
     * @param session CLI session
     */
    void resetCursor(CliSession session);
}
