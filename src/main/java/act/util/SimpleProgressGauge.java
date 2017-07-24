package act.util;

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

import act.Destroyable;
import org.osgl.$;
import org.osgl.util.E;

import java.util.ArrayList;
import java.util.List;

public class SimpleProgressGauge extends DestroyableBase implements ProgressGauge {

    private int maxHint;
    private int currentSteps;
    private ProgressGauge delegate;
    private List<Listener> listeners = new ArrayList<>();

    private SimpleProgressGauge(ProgressGauge delegate) {
        this.delegate = $.notNull(delegate);
    }

    public SimpleProgressGauge(int maxHint) {
        this.maxHint = maxHint;
    }

    public SimpleProgressGauge() {
        maxHint = 100;
    }

    @Override
    protected void releaseResources() {
        maxHint = 100;
        currentSteps = 0;
        Destroyable.Util.tryDestroy(delegate);
    }

    @Override
    public void addListener(Listener listener) {
        if (null != delegate) {
            delegate.addListener(listener);
        } else {
            listeners.add(listener);
        }
    }

    @Override
    public void updateMaxHint(int maxHint) {
        if (null != delegate) {
            delegate.updateMaxHint(maxHint);
        } else {
            this.maxHint = maxHint;
            triggerUpdateEvent();
        }
    }

    @Override
    public void step() {
        if (null != delegate) {
            delegate.step();
        } else {
            currentSteps++;
            triggerUpdateEvent();
        }
    }

    @Override
    public void stepBy(int steps) {
        if (null != delegate) {
            delegate.stepBy(steps);
        } else {
            currentSteps += steps;
            triggerUpdateEvent();
        }
    }

    @Override
    public void stepTo(int steps) {
        E.illegalArgumentIf(steps < 0);
        if (null != delegate) {
            delegate.stepTo(steps);
        } else {
            currentSteps = steps;
            triggerUpdateEvent();
        }
    }

    @Override
    public int currentSteps() {
        if (null != delegate) {
            return delegate.currentSteps();
        }
        return currentSteps;
    }

    @Override
    public int maxHint() {
        if (null != delegate) {
            return delegate.maxHint();
        }
        return maxHint;
    }

    public int currrentProgressPercent() {
        if (null != delegate) {
            return delegate.currentSteps() * 100 / delegate.maxHint();
        }
        return currentSteps * 100 / maxHint;
    }

    @Override
    public boolean done() {
        if (null != delegate) {
            return delegate.done();
        }
        return currentSteps == maxHint;
    }

    private void triggerUpdateEvent() {
        for (Listener listener : listeners) {
            listener.onUpdate(this);
        }
    }

    public static SimpleProgressGauge wrap(ProgressGauge progressGauge) {
        E.NPE(progressGauge);
        if (progressGauge instanceof SimpleProgressGauge) {
            return (SimpleProgressGauge) progressGauge;
        }
        return new SimpleProgressGauge(progressGauge);
    }

    public static String wsJobProgressTag(String jobId) {
        return "__act_job_progress_" + jobId + "__";
    }
}
