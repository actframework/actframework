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

import act.mail.MailerContext;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;

public abstract class MailerViewVarDef extends VarDef {
    protected MailerViewVarDef(String name, Class<?> type) {
        super(name, type);
    }

    protected MailerViewVarDef(String name, BeanSpec spec) {
        super(name, spec);
    }

    @Override
    public final Object evaluate(ActContext context) {
        return eval((MailerContext) context);
    }

    public abstract Object eval(MailerContext context);
}
