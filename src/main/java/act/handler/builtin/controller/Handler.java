package act.handler.builtin.controller;

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

import act.annotations.Order;
import act.security.CORS;
import act.util.LogSupportedDestroyableBase;
import act.util.Ordered;

/**
 * The base class of @Before, @After, @Exception, @Finally interceptor and
 * request dispatcher
 */
public abstract class Handler<T extends Handler> extends LogSupportedDestroyableBase implements Ordered {

    private Integer priority;

    protected Handler(Integer priority) {
        this.priority = priority;
    }

    public Integer priority() {
        return priority;
    }

    @Override
    public int order() {
        return null == priority ? Order.HIGHEST_PRECEDENCE : priority;
    }

    public void accept(Visitor visitor) {}

    public abstract boolean sessionFree();

    public abstract boolean express();

    public abstract boolean skipEvents();

    public interface Visitor {
        ActionHandlerInvoker.Visitor invokerVisitor();
    }

    @Override
    protected void releaseResources() {
    }

    public abstract CORS.Spec corsSpec();

}
