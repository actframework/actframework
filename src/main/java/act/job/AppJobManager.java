package act.job;

import act.app.App;
import act.app.AppServiceBase;
import act.app.AppThreadFactory;
import act.app.event.*;
import act.event.AppEventListenerBase;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.EventObject;
import java.util.Map;
import java.util.concurrent.*;

public class AppJobManager extends AppServiceBase<AppJobManager> {

    private ScheduledThreadPoolExecutor executor;
    private Map<String, _Job> jobs = C.newMap();

    static String appEventJobId(AppEventId eventId) {
        return S.builder("__act_app_").append(eventId.toString().toLowerCase()).toString();
    }

    public AppJobManager(App app) {
        super(app);
        initExecutor(app);
        for (AppEventId appEventId : AppEventId.values()) {
            final String jobId = appEventJobId(appEventId);
            addJob(new _Job(jobId, this));
            app.eventBus().bind(appEventId, new AppEventListenerBase() {
                @Override
                public void on(EventObject event) throws Exception {
                    jobs.get(jobId).run();
                }
            });
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
        jobById(appEventJobId(appEvent)).addPrecedenceJob(_Job.once(runnable, this));
    }

    public void post(AppEventId appEvent, final Runnable runnable) {
        jobById(appEventJobId(appEvent)).addFollowingJob(_Job.once(runnable, this));
    }

    public void on(AppEventId appEvent, String jobId, final Runnable runnable) {
        jobById(appEventJobId(appEvent)).addPrecedenceJob(_Job.once(jobId, runnable, this));
    }

    public void post(AppEventId appEvent, String jobId, final Runnable runnable) {
        jobById(appEventJobId(appEvent)).addFollowingJob(_Job.once(jobId, runnable, this));
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

    public _Job jobById(String id) {
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
    }

}
