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

import act.app.App;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Allow user to use TAB to auto complete command name
 */
@Singleton
public class CommandNameCompleter extends StringsCompleter implements Completer {

    @Inject
    public CommandNameCompleter(App app) {
        super(allCommandNames(app.cliDispatcher()));
    }

    private static List<String> allCommandNames(CliDispatcher dispatcher) {
        List<String> l = new ArrayList<>();

        List<String> appCommands = dispatcher.commands(false, true);
        l.addAll(appCommands);

        List<String> sysCommands = dispatcher.commands(true, false);
        l.addAll(sysCommands);

        for (String sysCmd : sysCommands) {
            l.add(S.afterFirst(sysCmd, "act."));
        }
        return l;
    }
}
