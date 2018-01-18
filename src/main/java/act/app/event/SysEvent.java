package act.app.event;

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

import act.app.App;
import act.event.ActEvent;
import act.event.SystemEvent;

public abstract class SysEvent extends ActEvent<App> implements SystemEvent {

    private SysEventId id;

    public SysEvent(SysEventId id, App source) {
        super(source);
        this.id = id;
    }

    @Override
    public String toString() {
        return id.name();
    }

    public int id() {
        return id.ordinal();
    }

    @Override
    public Class<? extends ActEvent<App>> eventType() {
        return getClass();
    }
}
