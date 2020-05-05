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
import act.util.ActContext;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.util.ArrayList;
import java.util.List;

class CliVarArgumentLoader extends CliParamValueLoader {

    private final StringValueResolver resolver;
    private final Class componentType;

    CliVarArgumentLoader(Class componentType, StringValueResolver resolver) {
        this.componentType = componentType;
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundArgument(null);
    }

    @Override
    public String toString() {
        return S.concat("cli var arg loader[", bindName(), "]");
    }

    @Override
    public Object load(Object cached, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        List list = new ArrayList();
        CliContext.ParsingContext parsingContext = ctx.parsingContext();
        CommandLineParser parser = ctx.commandLine();
        while (parsingContext.hasArguments(parser)) {
            int id = ctx.parsingContext().curArgId().getAndIncrement();
            list.add(resolver.resolve(parser.argument(id)));
        }
        return ArrayLoader.listToArray(list, componentType);
    }

    @Override
    public String bindName() {
        return null;
    }
}
