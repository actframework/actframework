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
import act.cli.CliSession;
import act.cli.util.CliCursor;
import act.handler.CliHandlerBase;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.util.C;

import java.util.List;

public class IterateCursor extends CliHandlerBase {

    public static final IterateCursor INSTANCE = new IterateCursor();

    private IterateCursor() {}

    @Override
    public void handle(CliContext context) {
        CliCursor cursor = context.session().cursor();
        if (null == cursor) {
            context.println("no cursor");
        } else {
            try {
                cursor.output(context);
            } finally {
                PropertySpec.current.remove();
            }
        }
    }

    @Override
    public void resetCursor(CliSession session) {
        // do not reset cursor for this particular handler
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public $.T2<String, String> commandLine() {
        return $.T2("it", "iterate through cursor");
    }

    @Override
    public List<$.T2<String, String>> options() {
        return C.list();
    }

}
