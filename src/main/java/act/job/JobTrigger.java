package act.job;

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

import static act.app.event.SysEventId.START;
import static act.app.event.SysEventId.STOP;
import static act.job.JobManager.sysEventJobId;

import act.app.App;
import act.app.event.SysEventId;
import act.conf.AppConfig;
import act.event.SysEventListenerBase;
import fc.cron.CronExpression;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;
import org.rythmengine.utils.Time;

import java.util.EventObject;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A `JobTrigger` triggers a {@link Job} to be executed
 */
public abstract class JobTrigger {

    protected static final Logger LOGGER = LogManager.get(JobTrigger.class);

    protected Boolean oneTime;

    private JobTrigger(Boolean oneTime) {
        this.oneTime = oneTime;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    protected static boolean isTraceEnabled() {
        return LOGGER.isTraceEnabled();
    }

    protected static void trace(String msg, Object... args) {
        LOGGER.trace(msg, args);
    }

    final void register(Job job, JobManager manager) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("trigger on [%s]: %s", this, job);
        }
        if (null != oneTime && !oneTime) {
            job.setNonOneTime();
        }
        job.trigger(this);
        schedule(manager, job);
    }

    void scheduleFollowingCalls(JobManager manager, Job job) {}

    void schedule(JobManager manager, Job job) {}

    void traceSchedule(Job job) {
        if (isTraceEnabled()) {
            trace("trigger[%s] schedule job: %s", this, job);
        }
    }

    static JobTrigger of(AppConfig config, Cron anno) {
        String v = anno.value();
        if (v.startsWith("cron.")) {
            v = (String) config.get(v);
        } else if (v.startsWith("${") && v.endsWith("}")) {
            v = v.substring(2, v.length() - 1);
            v = (String) config.get(v);
        }
        if (S.blank(v)) {
            throw E.invalidConfiguration("Cannot find configuration for cron: %s", anno.value());
        }
        return cron(v);
    }

    static JobTrigger of(AppConfig config, OnAppStart anno) {
        int delayInSeconds = anno.delayInSeconds();
        if (delayInSeconds > 0) {
            return delayAfter(START, delayInSeconds);
        }
        if (anno.async()) {
            return alongWith(START);
        } else {
            return after(START);
        }
    }

    static JobTrigger of(AppConfig config, OnAppStop anno) {
        if (anno.async()) {
            return alongWith(STOP);
        } else {
            return before(STOP);
        }
    }

    static JobTrigger of(AppConfig config, FixedDelay anno) {
        String delay = anno.value();
        if (delay.startsWith("delay.")) {
            delay = (String) config.get(delay);
        } else if (delay.startsWith("${") && delay.endsWith("}")) {
            delay = delay.substring(2, delay.length() - 1);
            delay = (String) config.get(delay);
        }
        if (S.blank(delay)) {
            throw E.invalidConfiguration("Cannot find configuration for delay: %s", anno.value());
        }
        return fixedDelay(delay, anno.startImmediately());
    }

    static JobTrigger of(AppConfig config, Every anno) {
        String duration = anno.value();
        if (duration.startsWith("every.")) {
            duration = (String) config.get(duration);
        } else if (duration.startsWith("${") && duration.endsWith("}")) {
            duration = duration.substring(2, duration.length() - 1);
            duration = (String) config.get(duration);
        }
        if (S.blank(duration)) {
            throw E.invalidConfiguration("Cannot find configuration for duration: %s", anno.value());
        }
        return every(duration, anno.startImmediately());
    }

    static JobTrigger of(AppConfig config, AlongWith anno) {
        String id = anno.value();
        E.illegalArgumentIf(S.blank(id), "associate job ID cannot be empty");
        int delayInSeconds = anno.delayInSeconds();
        if (delayInSeconds > 0) {
            return new _DelayAfter(id, delayInSeconds);
        } else {
            return new _AlongWith(id);
        }
    }

    static JobTrigger of(AppConfig config, InvokeAfter anno) {
        String id = anno.value();
        E.illegalArgumentIf(S.blank(id), "associate job ID cannot be empty");
        return new _After(id);
    }

    static JobTrigger of(AppConfig config, InvokeBefore anno) {
        String id = anno.value();
        E.illegalArgumentIf(S.blank(id), "associate job ID cannot be empty");
        return new _Before(id);
    }

    static JobTrigger cron(String expression) {
        return new _Cron(expression);
    }

    static JobTrigger fixedDelay(String duration, boolean startImmediately) {
        return new _FixedDelay(duration, startImmediately);
    }

    static JobTrigger fixedDelay(long seconds, boolean startImmediately) {
        return new _FixedDelay(seconds, startImmediately);
    }

    static JobTrigger fixedDelay(long interval, TimeUnit timeUnit, boolean startImmediately) {
        return new _FixedDelay(timeUnit.toSeconds(interval), startImmediately);
    }

    static JobTrigger every(String duration, boolean startImmediately) {
        return new _Every(duration, startImmediately);
    }

    static JobTrigger every(long seconds, boolean startImmediately) {
        return new _Every(seconds, TimeUnit.SECONDS, startImmediately);
    }

    static JobTrigger every(long duration, TimeUnit timeUnit, boolean startImmediately) {
        return new _Every(duration, timeUnit, startImmediately);
    }

    static JobTrigger onAppStart(boolean async, int delayInSeconds) {
        if (delayInSeconds > 0) {
            return delayAfter(START, delayInSeconds);
        }
        return async ? alongWith(START) : after(START);
    }

    static JobTrigger onAppStop(boolean async) {
        return async ? alongWith(STOP) : before(STOP);
    }

    static JobTrigger onSysEvent(SysEventId eventId, boolean async) {
        return async ? alongWith(eventId) : after(eventId);
    }

    static JobTrigger delayForSeconds(long seconds, boolean startImmediately) {
        return new _FixedDelay(seconds, startImmediately);
    }

    static JobTrigger alongWith(String jobId) {
        return new _AlongWith(jobId);
    }

    static JobTrigger alongWith(SysEventId sysEvent) {
        return new _AlongWith(sysEventJobId(sysEvent));
    }

    static JobTrigger before(String jobId) {
        return new _Before(jobId);
    }

    static JobTrigger before(SysEventId sysEvent) {
        return before(sysEventJobId(sysEvent));
    }

    static JobTrigger after(String jobId) {
        return new _After(jobId);
    }

    static JobTrigger after(SysEventId sysEvent) {
        return after(sysEventJobId(sysEvent));
    }

    static JobTrigger delayAfter(String jobId, int delayInSeconds) {
        return new _DelayAfter(jobId, delayInSeconds);
    }

    static JobTrigger delayAfter(SysEventId sysEvent, int delayInSeconds) {
        return delayAfter(sysEventJobId(sysEvent), delayInSeconds);
    }

    static class _Cron extends JobTrigger {
        private CronExpression cronExpr;
        _Cron(String expression) {
            super(false);
            cronExpr = new CronExpression(expression);
        }

        @Override
        public String toString() {
            return S.newBuffer("cron :").a(cronExpr).toString();
        }

        @Override
        void schedule(final JobManager manager, final Job job) {
            traceSchedule(job);
            App app = manager.app();
            if (!app.isStarted()) {
                app.eventBus().bindAsync(SysEventId.POST_START, new SysEventListenerBase() {
                    @Override
                    public void on(EventObject event) throws Exception {
                        delayedSchedule(manager, job);
                    }
                });
            } else {
                delayedSchedule(manager, job);
            }
        }

        private void delayedSchedule(JobManager manager, Job job) {
            DateTime now = DateTime.now();
            // add one seconds to prevent the next time be the current time (now)
            DateTime next = cronExpr.nextTimeAfter(now.plusSeconds(1));
            Seconds seconds = Seconds.secondsBetween(now, next);
            ScheduledFuture future = manager.executor().schedule(job, seconds.getSeconds(), TimeUnit.SECONDS);
            manager.futureScheduled(job.id(), future);
        }

        @Override
        void scheduleFollowingCalls(JobManager manager, Job job) {
            schedule(manager, job);
        }
    }

    private abstract static class _Periodical extends JobTrigger {
        protected long seconds;
        protected boolean startImmediately;
        _Periodical(String duration, boolean startImmediately) {
            super(false);
            E.illegalArgumentIf(S.blank(duration), "delay duration shall not be empty");
            seconds = Time.parseDuration(duration);
            E.illegalArgumentIf(seconds < 1, "delay duration shall not be zero or negative number");
            this.startImmediately = startImmediately;
        }
        _Periodical(long seconds, boolean startImmediately) {
            super(false);
            E.illegalArgumentIf(seconds < 1, "delay duration cannot be zero or negative");
            this.seconds = seconds;
            this.startImmediately = startImmediately;
        }

        @Override
        final void schedule(final JobManager manager, final Job job) {
            traceSchedule(job);
            App app = manager.app();
            if (!app.isStarted()) {
                app.eventBus().bindAsync(SysEventId.POST_START, new SysEventListenerBase() {
                    @Override
                    public void on(EventObject event) {
                        runAndSchedule(manager, job);
                    }
                });
            } else {
                runAndSchedule(manager, job);
            }
        }

        protected abstract void delayedSchedule(JobManager manager, Job job);

        protected void runAndSchedule(JobManager manager, Job job) {
            if (startImmediately) {
                manager.now(job);
            }
            delayedSchedule(manager, job);
        }

    }

    private static class _FixedDelay extends _Periodical {
        _FixedDelay(String duration, boolean startImmediately) {
            super(duration, startImmediately);
        }
        _FixedDelay(long seconds, boolean startImmediately) {
            super(seconds, startImmediately);
        }

        @Override
        public String toString() {
            return S.concat("fixed delay of ", S.string(seconds), " seconds");
        }

        @Override
        protected void delayedSchedule(JobManager manager, Job job) {
            ScheduledThreadPoolExecutor executor = manager.executor();
            ScheduledFuture future = executor.scheduleWithFixedDelay(job, seconds, seconds, TimeUnit.SECONDS);
            manager.futureScheduled(job.id(), future);
        }
    }

    private static class _Every extends _Periodical {
        _Every(String duration, boolean startImmediately) {
            super(duration, startImmediately);
        }

        _Every(long duration, TimeUnit timeUnit, boolean startImmediately) {
            super(timeUnit.toSeconds(duration), startImmediately);
        }

        @Override
        public String toString() {
            return S.concat("every ", S.string(seconds), " seconds");
        }

        @Override
        protected void delayedSchedule(JobManager manager, Job job) {
            ScheduledThreadPoolExecutor executor = manager.executor();
            ScheduledFuture future = executor.scheduleAtFixedRate(job, seconds, seconds, TimeUnit.SECONDS);
            manager.futureScheduled(job.id(), future);
        }
    }

    private abstract static class _AssociatedTo extends JobTrigger {
        String targetId;
        _AssociatedTo(String targetId) {
            super(null);
            E.illegalArgumentIf(S.blank(targetId), "associate job ID expected");
            this.targetId = targetId;
        }

        @Override
        void schedule(JobManager manager, Job job) {
            traceSchedule(job);
            Job associateTarget = manager.jobById(targetId, false);
            if (null == associateTarget) {
                LOGGER.warn("Failed to register job because target job not found: %s. Will try again after app started", targetId);
                scheduleDelayedRegister(manager, job);
            } else {
                associate(job, associateTarget);
            }
        }

        private void scheduleDelayedRegister(final JobManager manager, final Job job) {
            final String id = delayedRegisterJobId(job);
            before(START).register(new Job(id, manager, new $.F0<Void>() {
                @Override
                public Void apply() throws NotAppliedException, $.Break {
                    Job associateTo = manager.jobById(targetId);
                    if (null == associateTo) {
                        LOGGER.warn("Cannot find associated job: %s", id);
                    } else {
                        associate(job, associateTo);
                    }
                    return null;
                }
            }), manager);
        }

        private String delayedRegisterJobId(Job job) {
            return S.concat("delayed_association_register-", job.id(), "-to-", targetId);
        }

        abstract void associate(Job theJob, Job toJob);
    }

    private static class _AlongWith extends _AssociatedTo {
        _AlongWith(String targetId) {
            super(targetId);
        }

        @Override
        public String toString() {
            return S.concat("along with ", targetId);
        }

        @Override
        void associate(Job theJob, Job toJob) {
            toJob.addParallelJob(theJob);
        }
    }

    private static class _Before extends _AssociatedTo {
        _Before(String targetId) {
            super(targetId);
        }

        @Override
        public String toString() {
            return S.concat("before ", targetId);
        }

        @Override
        void associate(Job theJob, Job toJob) {
            toJob.addPrecedenceJob(theJob);
        }
    }

    private static class _After extends _AssociatedTo {
        _After(String targetId) {
            super(targetId);
        }

        @Override
        public String toString() {
            return S.concat("after ", targetId);
        }

        @Override
        void associate(Job theJob, Job toJob) {
            toJob.addFollowingJob(theJob);
        }
    }

    private static class _DelayAfter extends _AssociatedTo {

        private static final AtomicInteger seq = new AtomicInteger();

        private int delayInSeconds;

        _DelayAfter(String targetId, int delayInSeconds) {
            super(targetId);
            this.delayInSeconds = delayInSeconds;
        }

        @Override
        public String toString() {
            return S.concat("delay %ss after ", delayInSeconds, targetId);
        }

        @Override
        void associate(final Job theJob, final Job toJob) {
            toJob.addPrecedenceJob(new Job(toJob.id() + "-delay-" + delayInSeconds + "-" + seq.getAndIncrement(), toJob.manager()) {
                @Override
                public void run() {
                    toJob.manager().delay(theJob, delayInSeconds, TimeUnit.SECONDS);
                }
            });
        }
    }

}



