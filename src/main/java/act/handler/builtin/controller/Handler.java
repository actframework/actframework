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

import act.security.CORS;
import act.util.DestroyableBase;
import org.osgl.$;

/**
 * The base class of @Before, @After, @Exception, @Finally interceptor and
 * request dispatcher
 */
public abstract class Handler<T extends Handler> extends DestroyableBase implements Comparable<T> {

    private int priority;

    protected Handler(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(T o) {
        return priority - o.priority();
    }

    @Override
    public int hashCode() {
        return $.hc(priority, getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj.getClass().equals(getClass());
    }

    public void accept(Visitor visitor) {}

    public abstract boolean sessionFree();

    public abstract boolean express();

    public interface Visitor {
        ActionHandlerInvoker.Visitor invokerVisitor();
    }

    @Override
    protected void releaseResources() {
    }

    public abstract CORS.Spec corsSpec();
}
