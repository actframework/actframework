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

import act.Act;
import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.AppThreadFactory;
import act.app.event.AppEventId;
import act.event.AppEventListenerBase;
import act.event.OnceEventListenerBase;
import act.mail.MailerContext;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import org.rythmengine.utils.Time;

import java.util.EventObject;
import java.util.Map;
import java.util.concurrent.*;

public class AppJobManager extends AppServiceBase<AppJobManager> {

    private static final Logger LOGGER = LogManager.get(AppJobManager.class);

    private ScheduledThreadPoolExecutor executor;
    private ConcurrentMap<String, _Job> jobs = new ConcurrentHashMap<String, _Job>();
    private ConcurrentMap<String, ScheduledFuture> scheduled = new ConcurrentHashMap<>();

    static String appEventJobId(AppEventId eventId) {
        return S.concat("__act_app__", eventId.toString().toLowerCase());
    }

    public AppJobManager(App app) {
        super(app);
        initExecutor(app);
        for (AppEventId appEventId : AppEventId.values()) {
            createAppEventListener(appEventId);
        }
    }

    @Override
    protected void releaseResources() {
        LOGGER.trace("release job manager resources");
        for (_Job job : jobs.values()) {
            job.destroy();
        }
        jobs.clear();
        executor.getQueue().clear();
        executor.shutdownNow();
    }

    public <T> Future<T> now(Callable<T> callable) {
        return executor().submit(callable);
    }

    public void now(Runnable runnable) {
        executor().submit(wrap(runnable));
    }

    public <T> Future<T> delay(Callable<T> callable, long delay, TimeUnit timeUnit) {
        return executor().schedule(callable, delay, timeUnit);
    }

    public void delay(Runnable runnable, long delay, TimeUnit timeUnit) {
        executor().schedule(wrap(runnable), delay, timeUnit);
    }

    public <T> Future<T> delay(Callable<T> callable, String delay) {
        int seconds = parseTime(delay);
        return executor().schedule(callable, seconds, TimeUnit.SECONDS);
    }

    public void delay(Runnable runnable, String delay) {
        int seconds = parseTime(delay);
        executor().schedule(wrap(runnable), seconds, TimeUnit.SECONDS);
    }

    public void every(String id, Runnable runnable, String interval) {
        JobTrigger.every(interval).schedule(this, _Job.multipleTimes(id, runnable, this));
    }

    public void every(Runnable runnable, String interval) {
        JobTrigger.every(interval).schedule(this, _Job.multipleTimes(runnable, this));
    }

    public void every(Runnable runnable, long interval, TimeUnit timeUnit) {
        JobTrigger.every(interval, timeUnit).schedule(this, _Job.multipleTimes(runnable, this));
    }

    public void every(String id, Runnable runnable, long interval, TimeUnit timeUnit) {
        JobTrigger.every(interval, timeUnit).schedule(this, _Job.multipleTimes(id, runnable, this));
    }

    public void fixedDelay(Runnable runnable, String interval) {
        JobTrigger.every(interval).schedule(this, _Job.multipleTimes(runnable, this));
    }

    public void fixedDelay(String id, Runnable runnable, String interval) {
        JobTrigger.every(interval).schedule(this, _Job.multipleTimes(id, runnable, this));
    }

    public void fixedDelay(Runnable runnable, long interval, TimeUnit timeUnit) {
        JobTrigger.fixedDelay(interval, timeUnit).schedule(this, _Job.multipleTimes(runnable, this));
    }

    public void fixedDelay(String id, Runnable runnable, long interval, TimeUnit timeUnit) {
        JobTrigger.fixedDelay(interval, timeUnit).schedule(this, _Job.multipleTimes(id, runnable, this));
    }

    private int parseTime(String timeDuration) {
        if (timeDuration.startsWith("${") && timeDuration.endsWith("}")) {
            timeDuration = app().config().get(timeDuration.substring(2, timeDuration.length() - 1));
        }
        return Time.parseDuration(timeDuration);
    }

    public void on(DateTime instant, Runnable runnable) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("schedule runnable[%s] on %s", runnable, instant);
        }
        DateTime now = DateTime.now();
        E.illegalArgumentIf(instant.isBefore(now));
        Seconds seconds = Seconds.secondsBetween(now, instant);
        executor().schedule(wrap(runnable), seconds.getSeconds(), TimeUnit.SECONDS);
    }

    public <T> Future<T> on(DateTime instant, Callable<T> callable) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("schedule callable[%s] on %s", callable, instant);
        }
        DateTime now = DateTime.now();
        E.illegalArgumentIf(instant.isBefore(now));
        Seconds seconds = Seconds.secondsBetween(now, instant);
        return executor().schedule(callable, seconds.getSeconds(), TimeUnit.SECONDS);
    }

    public void on(AppEventId appEvent, final Runnable runnable) {
        on(appEvent, runnable, false);
    }

    public void on(AppEventId appEvent, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        on(appEvent, runnable.toString(), runnable, runImmediatelyIfEventDispatched);
    }

    public void post(AppEventId appEvent, final Runnable runnable) {
        post(appEvent, runnable, false);
    }

    public void post(AppEventId appEvent, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        _Job job = jobById(appEventJobId(appEvent));
        if (null == job) {
            processDelayedJob(wrap(runnable), runImmediatelyIfEventDispatched);
        } else {
            job.addFollowingJob(_Job.once(runnable, this));
        }
    }

    public void on(AppEventId appEvent, String jobId, final Runnable runnable) {
        on(appEvent, jobId, runnable, false);
    }

    public void on(AppEventId appEvent, String jobId, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        boolean traceEnabled = LOGGER.isTraceEnabled();
        if (traceEnabled) {
            LOGGER.trace("binding job[%s] to app event: %s, run immediately if event dispatched: %s", jobId, appEvent, runImmediatelyIfEventDispatched);
        }
        _Job job = jobById(appEventJobId(appEvent));
        if (null == job) {
            if (traceEnabled) {
                LOGGER.trace("process delayed job: %s", jobId);
            }
            processDelayedJob(wrap(runnable), runImmediatelyIfEventDispatched);
        } else {
            if (traceEnabled) {
                LOGGER.trace("schedule job: %s", jobId);
            }
            job.addPrecedenceJob(_Job.once(jobId, runnable, this));
        }
    }

    public void post(AppEventId appEvent, String jobId, final Runnable runnable) {
        post(appEvent, jobId, runnable, false);
    }

    public void post(AppEventId appEvent, String jobId, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        _Job job = jobById(appEventJobId(appEvent));
        if (null == job) {
            processDelayedJob(wrap(runnable), runImmediatelyIfEventDispatched);
        } else {
            job.addFollowingJob(_Job.once(jobId, runnable, this));
        }
    }

    private void processDelayedJob(final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        if (runImmediatelyIfEventDispatched) {
            try {
                runnable.run();
            } catch (Exception e) {
                Act.LOGGER.error(e, "Error running job");
            }
        } else {
            now(runnable);
        }
    }

    /**
     * Cancel a scheduled Job by ID
     * @param jobId the job Id
     */
    public void cancel(String jobId) {
        _Job job = jobById(jobId);
        if (null != job) {
            removeJob(job);
        } else {
            ScheduledFuture future = scheduled.remove(jobId);
            if (null != future) {
                future.cancel(true);
            }
        }
    }

    public void beforeAppStart(final Runnable runnable) {
        on(AppEventId.START, runnable);
    }

    public void afterAppStart(final Runnable runnable) {
        post(AppEventId.START, runnable);
    }

    public void beforeAppStop(final Runnable runnable) {
        on(AppEventId.STOP, runnable);
    }

    C.List<_Job> jobs() {
        return C.list(jobs.values());
    }

    C.List<_Job> virtualJobs() {
        final AppJobManager jobManager = Act.jobManager();
        return C.list(scheduled.entrySet()).map(new $.Transformer<Map.Entry<String, ScheduledFuture>, _Job>() {
            @Override
            public _Job transform(Map.Entry<String, ScheduledFuture> entry) {
                return new _Job(entry.getKey(), jobManager);
            }
        });
    }

    void futureScheduled(String id, ScheduledFuture future) {
        scheduled.putIfAbsent(id, future);
    }

    _Job jobById(String id) {
        _Job job = jobs.get(id);
        if (null == job) {
            ScheduledFuture future = scheduled.get(id);
            if (null != future) {
                return new _Job(id, Act.jobManager());
            }
            Act.LOGGER.warn("cannot find job by id: %s", id);
        }
        return job;
    }

    void addJob(_Job job) {
        jobs.put(job.id(), job);
    }

    void removeJob(_Job job) {
        String id = job.id();
        jobs.remove(id);
        ScheduledFuture future = scheduled.remove(id);
        if (null != future) {
            future.cancel(true);
        }
    }

    ScheduledThreadPoolExecutor executor() {
        return executor;
    }

    private void initExecutor(App app) {
        int poolSize = app.config().jobPoolSize();
        executor = new ScheduledThreadPoolExecutor(poolSize, new AppThreadFactory("jobs"), new ThreadPoolExecutor.AbortPolicy());
        executor.setRemoveOnCancelPolicy(true);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("init executor with thread pool: %s", poolSize);
        }
    }

    private void createAppEventListener(AppEventId appEventId) {
        String jobId = appEventJobId(appEventId);
        _Job job = new _Job(jobId, this);
        addJob(job);
        app().eventBus().bind(appEventId, new _AppEventListener(jobId, job));
    }

    private static class _AppEventListener extends AppEventListenerBase {
        private Runnable worker;
        _AppEventListener(String id, Runnable worker) {
            super(id);
            this.worker = $.NPE(worker);
        }

        @Override
        public void on(EventObject event) throws Exception {
            worker.run();
        }

        @Override
        protected void releaseResources() {
            if (null != worker && worker instanceof Destroyable) {
                ((Destroyable) worker).destroy();
            }
        }
    }

    private Runnable wrap(Runnable runnable) {
        return new ContextualJob(app().cuid(), runnable);
    }

    private Runnable wrap(final Callable callable) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw E.unexpected(e);
                }
            }
        };
    }

    private class ContextualJob extends _Job {

        private JobContext origin_ = JobContext.copy();

        ContextualJob(String id, final Runnable runnable) {
            super(id, AppJobManager.this, new $.F0() {
                @Override
                public Object apply() throws NotAppliedException, $.Break {
                    runnable.run();
                    return null;
                }
            }, true);
            app().eventBus().once(MailerContext.InitEvent.class, new OnceEventListenerBase<MailerContext.InitEvent>() {
                @Override
                public boolean tryHandle(MailerContext.InitEvent event) throws Exception {
                    MailerContext mailerContext = MailerContext.current();
                    if (null != mailerContext) {
                        _before();
                        return true;
                    }
                    return true;
                }
            });
        }

        @Override
        protected void _before() {
            // copy the JobContext of parent thread into the current thread
            JobContext.init(origin_);
        }

        @Override
        protected void _finally() {
            JobContext.clear();
        }
    }

}
