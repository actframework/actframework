package act.handler.builtin.controller;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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
import act.util.SimpleProgressGauge;

/**
 * Used for track async request handler method progress
 */
public class ControllerProgressGauge extends SimpleProgressGauge {

    @Override
    public void fail(String error) {
        this.error = error;
        // do not trigger update event yet
    }

    @Override
    public void stepBy(int steps) {
        currentSteps += steps;
        if (!isDone()) {
            triggerUpdateEvent();
        }
    }

    @Override
    public void stepTo(int steps) {
        if (currentSteps != steps) {
            currentSteps = steps;
            if (!isDone()) {
                triggerUpdateEvent();
            }
        }
    }

    public void commitFinalState() {
        if (null == this.error) {
            markAsDone();
        }
        triggerUpdateEvent(true);
    }
}
