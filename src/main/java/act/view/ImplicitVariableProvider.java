package act.view;

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
import act.plugin.Plugin;

import java.util.List;

/**
 * Plugin developer could extend this interface to inject
 * implicit variables to view template
 */
public abstract class ImplicitVariableProvider implements Plugin {

    /**
     * Returns a list of implicit variables the plugin needs to inject
     * into template render arguments for action view
     */
    public abstract List<ActionViewVarDef> implicitActionViewVariables();

    /**
     * Returns a list of implicit variables the plugin needs to inject
     * into template render arguments for mailer view
     */
    public abstract List<MailerViewVarDef> implicitMailerViewVariables();

    @Override
    public void register() {
        Act.viewManager().register(this);
    }
}
