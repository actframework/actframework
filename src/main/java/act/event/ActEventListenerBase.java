package act.event;

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

import act.app.event.SysEventListener;
import act.util.LogSupportedDestroyableBase;
import org.osgl.util.S;

import java.util.EventObject;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ActEventListenerBase<EVENT_TYPE extends EventObject> extends LogSupportedDestroyableBase implements ActEventListener<EVENT_TYPE> {

    private static final AtomicInteger ID_ = new AtomicInteger();

    private String id;
    public ActEventListenerBase(CharSequence id) {
        if (null == id) {
            id = genId();
        }
        this.id = id.toString();
    }

    public ActEventListenerBase() {
        this(genId());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SysEventListener) {
            SysEventListener that = (SysEventListener) obj;
            return S.eq(that.id(), this.id());
        }
        return false;
    }

    private static String genId() {
        return S.random(3) + ID_.getAndIncrement();
    }

}
