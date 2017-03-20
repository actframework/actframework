package act.cli.builtin;

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

import act.cli.CliContext;
import act.handler.CliHandlerBase;
import org.osgl.$;
import org.osgl.util.C;

import java.util.List;

public class Exit extends CliHandlerBase {

    public static final Exit INSTANCE = new Exit();
    public static final $.Break BREAK = new $.Break(true);

    private Exit() {}

    @Override
    public void handle(CliContext context) {
        context.println("bye");
        context.flush();
        throw BREAK;
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public $.T2<String, String> commandLine() {
        return $.T2("exit", "exit the console");
    }

    @Override
    public List<$.T2<String, String>> options() {
        return C.list();
    }

}
