package act.cli.event;

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

import act.cli.CliSession;
import act.event.ActEvent;
import act.event.SystemEvent;

/**
 * This event is emitted synchronously when a {@link CliSession} begin to run
 */
public class CliSessionStart extends ActEvent<CliSession> implements SystemEvent {
    public CliSessionStart(CliSession source) {
        super(source);
    }

    @Override
    public Class<? extends ActEvent<CliSession>> eventType() {
        return CliSessionStart.class;
    }
}
