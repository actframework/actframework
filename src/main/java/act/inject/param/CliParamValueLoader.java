package act.inject.param;

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
import act.cli.util.CommandLineParser;

import java.util.List;

abstract class CliParamValueLoader extends ParamValueLoader.Cacheable {

    final String optionValue(String lead1, String lead2, CliContext ctx) {
        return parser(ctx).getString(lead1, lead2);
    }

    final boolean multipleParams(CliContext context) {
        return context.parsingContext().hasMultipleOptionArguments();
    }

    protected final Object sessionVal(String bindName, CliContext context) {
        return context.attribute(bindName);
    }

    protected final String getFirstArgument(CliContext context) {
        List<String> list = arguments(context);
        return list.isEmpty() ? null : list.get(0);
    }

    protected final List<String> arguments(CliContext context) {
        return context.arguments();
    }

    private CommandLineParser parser(CliContext ctx) {
        return ctx.commandLine();
    }

}
