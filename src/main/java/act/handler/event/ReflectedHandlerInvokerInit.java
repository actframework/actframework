package act.handler.event;

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

import act.event.ActEvent;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;

/**
 * Emitted when {@link ReflectedHandlerInvoker} is initialized.
 *
 * 3rd part plugin or application can listen to this event and do further initialization work
 * to the reflected handler invoker, for example, checking on annotations and set appropriate
 * attributes.
 */
public class ReflectedHandlerInvokerInit extends ActEvent<ReflectedHandlerInvoker> {
    public ReflectedHandlerInvokerInit(ReflectedHandlerInvoker source) {
        super(source);
    }
}
