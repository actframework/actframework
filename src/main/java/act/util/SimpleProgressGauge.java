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
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.*;

public class SimpleProgressGauge extends DestroyableBase implements ProgressGauge {

    public static final ProgressGauge NULL = new SimpleProgressGauge(100, -1) {

        @Override
        public void addListener(Listener listener) {
            E.unsupport();
        }

        @Override
        public void updateMaxHint(int maxHint) {
            E.unsupport();
        }

        @Override
        public void incrMaxHint() {
            E.unsupport();
        }

        @Override
        public void incrMaxHintBy(int number) {
            E.unsupport();
        }

        @Override
        public void step() {
            E.unsupport();
        }

        @Override
        public void stepBy(int steps) {
            E.unsupport();
        }

        @Override
        public void stepTo(int steps) {
            E.unsupport();
        }

        @Override
        public void setId(String id) {
            E.unsupport();
        }

        @Override
        public void markAsDone() {
            E.unsupport();
        }
    };

    private String id;
    private boolean markedAsDown;
    private int maxHint;
    private int currentSteps;
    private String error;
    private Map<String, Object> payload = new HashMap<>();
    private transient int percent;
    private ProgressGauge delegate;
    private List<Listener> listeners = new ArrayList<>();
    private ReadWriteLock listenerListLock = new ReentrantReadWriteLock();

    private SimpleProgressGauge(int maxHint, int currentSteps) {
        this.maxHint = maxHint;
        this.currentSteps = currentSteps;
    }

    private SimpleProgressGauge(ProgressGauge delegate) {
        this.delegate = $.requireNotNull(delegate);
    }

    public SimpleProgressGauge(int maxHint) {
        this.maxHint = maxHint;
    }

    public SimpleProgressGauge() {
        maxHint = 100;
    }

    @Override
    protected void releaseResources() {
        listeners.clear();
        payload.clear();
        Destroyable.Util.tryDestroy(delegate);
    }

    @Override
    public void addListener(Listener listener) {
        if (null != delegate) {
            delegate.addListener(listener);
        } else {
            Lock lock = listenerListLock.writeLock();
            lock.lock();
            try {
                listeners.add(listener);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void updateMaxHint(int maxHint) {
        if (null != delegate) {
            delegate.updateMaxHint(maxHint);
        } else {
            this.maxHint = maxHint;
            triggerUpdateEvent(true);
        }
    }

    @Override
    public void incrMaxHint() {
        this.maxHint++;
    }

    @Override
    public void incrMaxHintBy(int number) {
        this.maxHint += number;
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
            if (currentSteps != steps) {
                currentSteps = steps;
                triggerUpdateEvent();
            }
        }
    }

    @Override
    public int currentSteps() {
        if (null != delegate) {
            return delegate.currentSteps();
        }
        return currentSteps;
    }

    public int getCurrentSteps() {
        return currentSteps();
    }

    @Override
    public int maxHint() {
        if (null != delegate) {
            return delegate.maxHint();
        }
        return maxHint;
    }

    public int getMaxHint() {
        return maxHint();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public int currentProgressPercent() {
        if (null != delegate) {
            return percentage(delegate.currentSteps(), delegate.maxHint());
        }
        return percentage(currentSteps, maxHint);
    }

    public int getProgressPercent() {
        return currentProgressPercent();
    }

    @Override
    public boolean isDone() {
        if (isDestroyed()) {
            return true;
        }
        if (null != delegate) {
            return delegate.isDone();
        }
        return null != error || currentSteps >= (maxHint - 1);
    }

    public void fail(String error) {
        this.error = error;
        triggerUpdateEvent(true);
    }

    public String error() {
        return error;
    }

    public boolean hasError() {
        return S.notBlank(error);
    }

    @Override
    public void markAsDone() {
        if (!markedAsDown) {
            markedAsDown = true;
            stepTo(maxHint);
        }
    }

    @Override
    public void clearPayload() {
        this.payload.clear();
    }

    @Override
    public void setPayload(String key, Object val) {
        this.payload.put(key, val);
        this.triggerUpdateEvent(true);
    }

    @Override
    public Map<String, Object> getPayload() {
        return this.payload;
    }

    private void triggerUpdateEvent() {
        triggerUpdateEvent(false);
    }

    private void triggerUpdateEvent(boolean forceTriggerEvent) {
        if (forceTriggerEvent || percentageChanged()) {
            Lock lock = listenerListLock.readLock();
            lock.lock();
            try {
                for (Listener listener : listeners) {
                    listener.onUpdate(this);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean percentageChanged() {
        int cur = currentProgressPercent();
        if (cur != percent) {
            percent = cur;
            return true;
        }
        return false;
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

    private static int percentage(int currentSteps, int maxHint) {
        int n = currentSteps * 100 / maxHint;
        return (100 <= n) && (currentSteps < (maxHint - 1)) ? 99 : n;
    }
}
