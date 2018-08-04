package act.plugin;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.app.ActionContext;

import javax.inject.Provider;

/**
 * Provide the current user principal name.
 */
public interface PrincipalProvider extends Provider<String> {

    /**
     * The default implementation of {@link PrincipalProvider}.
     *
     * It fetch the principal name from {@link ActionContext#username()}
     */
    class DefaultPrincipalProvider implements PrincipalProvider {

        public static final PrincipalProvider INSTANCE = new DefaultPrincipalProvider();

        private DefaultPrincipalProvider() {}

        @Override
        public String get() {
            ActionContext context = ActionContext.current();
            return null == context ? null : context.username();
        }
    }
}
