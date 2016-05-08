package act.job;

import act.ActComponent;
import act.Destroyable;
import act.app.App;
import act.app.AppServiceBase;
import act.app.AppThreadFactory;
import act.app.event.AppEventId;
import act.event.AppEventListenerBase;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import org.rythmengine.utils.Time;

import java.util.EventObject;
import java.util.concurrent.*;

@ActComponent
public class AppJobManager extends AppServiceBase<AppJobManager> {

    private ScheduledThreadPoolExecutor executor;
    private ConcurrentMap<String, _Job> jobs = new ConcurrentHashMap<String, _Job>();

    static String appEventJobId(AppEventId eventId) {
        return S.builder("__act_app_").append(eventId.toString().toLowerCase()).toString();
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
        for (_Job job : jobs.values()) {
            job.destroy();
        }
        jobs.clear();
        executor.shutdown();
        executor.getQueue().clear();
    }

    public <T> Future<T> now(Callable<T> callable) {
        return executor().submit(callable);
    }

    public void now(Runnable runnable) {
        executor().submit(runnable);
    }

    public <T> Future<T> delay(Callable<T> callable, long delay, TimeUnit timeUnit) {
        return executor().schedule(callable, delay, timeUnit);
    }

    public void delay(Runnable runnable, long delay, TimeUnit timeUnit) {
        executor().schedule(runnable, delay, timeUnit);
    }

    public <T> Future<T> delay(Callable<T> callable, String delay) {
        int seconds = parseTime(delay);
        return executor().schedule(callable, seconds, TimeUnit.SECONDS);
    }

    public void delay(Runnable runnable, String delay) {
        int seconds = parseTime(delay);
        executor().schedule(runnable, seconds, TimeUnit.SECONDS);
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
            timeDuration = (String) app().config().get(timeDuration.substring(2, timeDuration.length() - 1));
        }
        return Time.parseDuration(timeDuration);
    }

    public void on(DateTime instant, Runnable runnable) {
        DateTime now = DateTime.now();
        E.illegalArgumentIf(instant.isBefore(now));
        Seconds seconds = Seconds.secondsBetween(now, instant);
        executor().schedule(runnable, seconds.getSeconds(), TimeUnit.SECONDS);
    }

    public <T> Future<T> on(DateTime instant, Callable<T> callable) {
        DateTime now = DateTime.now();
        E.illegalArgumentIf(instant.isBefore(now));
        Seconds seconds = Seconds.secondsBetween(now, instant);
        return executor().schedule(callable, seconds.getSeconds(), TimeUnit.SECONDS);
    }

    public void on(AppEventId appEvent, final Runnable runnable) {
        on(appEvent, runnable, false);
    }

    public void on(AppEventId appEvent, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        _Job job = jobById(appEventJobId(appEvent));
        if (null == job) {
            processDelayedJob(runnable, runImmediatelyIfEventDispatched);
        } else {
            job.addPrecedenceJob(_Job.once(runnable, this));
        }
    }

    public void post(AppEventId appEvent, final Runnable runnable) {
        post(appEvent, runnable, false);
    }

    public void post(AppEventId appEvent, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        _Job job = jobById(appEventJobId(appEvent));
        if (null == job) {
            processDelayedJob(runnable, runImmediatelyIfEventDispatched);
        } else {
            job.addFollowingJob(_Job.once(runnable, this));
        }
    }

    public void on(AppEventId appEvent, String jobId, final Runnable runnable) {
        on(appEvent, jobId, runnable, false);
    }

    public void on(AppEventId appEvent, String jobId, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        _Job job = jobById(appEventJobId(appEvent));
        if (null == job) {
            processDelayedJob(runnable, runImmediatelyIfEventDispatched);
        } else {
            job.addPrecedenceJob(_Job.once(jobId, runnable, this));
        }
    }

    public void post(AppEventId appEvent, String jobId, final Runnable runnable) {
        post(appEvent, jobId, runnable, false);
    }

    public void post(AppEventId appEvent, String jobId, final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        _Job job = jobById(appEventJobId(appEvent));
        if (null == job) {
            processDelayedJob(runnable, runImmediatelyIfEventDispatched);
        } else {
            job.addFollowingJob(_Job.once(jobId, runnable, this));
        }
    }

    private void processDelayedJob(final Runnable runnable, boolean runImmediatelyIfEventDispatched) {
        if (runImmediatelyIfEventDispatched) {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.error(e, "Error running job");
            }
        } else {
            now(runnable);
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

    _Job jobById(String id) {
        _Job job = jobs.get(id);
        if (null == job) {
            logger.warn("cannot find job by id: %s", id);
        }
        return job;
    }

    void addJob(_Job job) {
        jobs.put(job.id(), job);
    }

    void removeJob(_Job job) {
        jobs.remove(job.id());
    }

    ScheduledThreadPoolExecutor executor() {
        return executor;
    }

    private void initExecutor(App app) {
        int poolSize = app.config().jobPoolSize();
        executor = new ScheduledThreadPoolExecutor(poolSize, new AppThreadFactory("jobs"), new ThreadPoolExecutor.AbortPolicy());
        executor.setRemoveOnCancelPolicy(true);
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

    private void createAppEventListener(AppEventId appEventId) {
        String jobId = appEventJobId(appEventId);
        _Job job = new _Job(jobId, this);
        addJob(job);
        app().eventBus().bind(appEventId, new _AppEventListener(jobId, job));
    }
}
