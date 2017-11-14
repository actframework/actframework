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

/**
 * Responsible for providing the value to a method parameter
 */
public interface ParamValueLoader {

    ParamValueLoader NIL = new ParamValueLoader() {
        @Override
        public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
            return null;
        }

        @Override
        public String bindName() {
            return null;
        }
    };

    /**
     * Provide the value for a parameter from current execution context.
     *
     * The context could be one of
     *
     * * {@link act.app.ActionContext}
     * * {@link CliContext}
     *
     * @param bean    the bean that cached for current session or `null` if not applied
     * @param context the current execution context
     * @param noDefaultValue if `true` then it shall not load default value when not provided by request
     * @return the value object
     */
    Object load(Object bean, ActContext<?> context, boolean noDefaultValue);

    /**
     * Returns the parameter binding name
     * @return the bind name
     */
    String bindName();

}
