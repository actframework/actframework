package act.sys;

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
import act.event.SystemPausedEvent;
import com.sun.management.GcInfo;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SystemAvailabilityMonitor extends $.Visitor<GcInfo> implements Runnable {

    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.get(SystemAvailabilityMonitor.class);

    private static final AtomicBoolean available = new AtomicBoolean(true);

    private static final int CHECK_PERIOD = 200;

    // used to queue last 2s gc counters
    private Deque<AtomicInteger> gcCounters = new ArrayDeque<>();

    // count for recent 200ms gc count
    private AtomicInteger currentCounter = new AtomicInteger();

    // count for recent 2s gc count
    private AtomicInteger smallWindowSum = new AtomicInteger();

    private int largeWindowOffset = 0;

    private int largeWindowCycle = 0;

    private AtomicBoolean active = new AtomicBoolean();

    // count for recent 4s gc count
    private AtomicInteger largeWindowSum = new AtomicInteger();

    @Override
    public void visit(GcInfo gcInfo) throws Lang.Break {
        currentCounter.incrementAndGet();
        if (smallWindowSum.incrementAndGet() > 10) {
            // there are at least 11 gc happened within last 2s
            pauseNow();
            wakeUp();
        }
        largeWindowSum.incrementAndGet();
        LOGGER.debug("gc notified. gc summary: %s | %s", smallWindowSum.get(), largeWindowSum.get());
    }

    private void shiftGcCounters() {
        // push current counter into 2s window
        for (; ; ) {
            int current = currentCounter.get();
            if (currentCounter.compareAndSet(current, 0)) {
                gcCounters.addLast(new AtomicInteger(current));
                break;
            }
        }
        // in case the window is full, we poll the first out
        if (gcCounters.size() > 10) {
            AtomicInteger shiftOut = gcCounters.poll();
            int n = shiftOut.get();
            if (n > 0) {
                // the shift out has gc count, we need to
                // deduct from the small window sum and
                // add up to large window offset holder
                for (; ; ) {
                    int current = smallWindowSum.get();
                    int next = current - n;
                    if (smallWindowSum.compareAndSet(current, next)) {
                        largeWindowOffset += n;
                        break;
                    }
                }
            }
        }
        // every 4s we apply the large window offset to the large window sum
        if ((++largeWindowCycle) % 20 == 0) {
            largeWindowCycle = 0;
            for (; ; ) {
                int current = largeWindowSum.get();
                int next = current - largeWindowOffset;
                if (largeWindowSum.compareAndSet(current, next)) {
                    largeWindowOffset = 0;
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            shiftGcCounters();
            int gcSum = largeWindowSum.get();
            if (gcSum == 0) {
                resumeNow();
                hibernate();
            }
            if (active.get()) {
                executor.schedule(this, CHECK_PERIOD, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void wakeUp() {
        if (active.compareAndSet(false, true)) {
            LOGGER.info("wake up system availability monitor");
            executor.schedule(this, CHECK_PERIOD, TimeUnit.MILLISECONDS);
        }
    }

    private void hibernate() {
        if (active.compareAndSet(true, false)) {
            LOGGER.info("system availability monitor hibernated");
        }
    }

    private static boolean resumeNow() {
        if (available.compareAndSet(false, true)) {
            LOGGER.info("Service recovered");
            return true;
        }
        return false;
    }

    public static boolean pauseNow() {
        if (available.compareAndSet(true, false)) {
            LOGGER.info("Service paused");
            Act.eventBus().trigger(SystemPausedEvent.INSTANCE);
            return true;
        }
        return false;
    }

    public static boolean isAvailable() {
        return available.get();
    }

    public static void start() {
        SystemAvailabilityMonitor monitor = new SystemAvailabilityMonitor();
        GcHelper.registerGcEventListener(monitor);
    }
}
