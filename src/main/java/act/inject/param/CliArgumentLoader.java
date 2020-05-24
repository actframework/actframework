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
import act.util.ActContext;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

class CliArgumentLoader extends CliParamValueLoader {

    private final StringValueResolver resolver;

    CliArgumentLoader(StringValueResolver resolver, String defVal) {
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundArgument(defVal);
    }

    @Override
    public String toString() {
        return S.concat("cli argument loader[", bindName(), "]");
    }

    @Override
    public Object load(Object cached, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        CliContext.ParsingContext ptx = ctx.parsingContext();
        int id = ptx.curArgId().getAndIncrement();
        String optVal = ctx.commandLine().argument(id);
        Object val = null;
        if (S.isBlank(optVal)) {
            optVal = ptx.argDefVals.get(id);
        }
        if (S.notBlank(optVal)) {
            val = resolver.resolve(optVal);
        }
        if (null == val && null != cached) {
            val = cached;
        }
        return val;
    }

    @Override
    public String bindName() {
        return null;
    }
}
