package act.job;

import act.app.App;
import act.app.AppServiceBase;
import act.app.AppThreadFactory;
import act.app.event.AppStart;
import act.app.event.AppStop;
import act.event.AppEventListenerBase;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Map;
import java.util.concurrent.*;

import static act.app.event.AppEventId.START;
import static act.app.event.AppEventId.STOP;

public class AppJobManager extends AppServiceBase<AppJobManager> {

    public static final String JOB_APP_START = "__act_app_started";
    public static final String JOB_APP_SHUTDOWN = "__act_app_shutdown";

    private ScheduledThreadPoolExecutor executor;
    private Map<String, _Job> jobs = C.newMap();

    public AppJobManager(App app) {
        super(app);
        initExecutor(app);
        registerSysJobs();
        app.eventBus().bind(START, new AppEventListenerBase<AppStart>("job-mgr-start") {
            @Override
            public void on(AppStart event) {
                jobs.get(JOB_APP_START).run();
            }
        }).bind(STOP, new AppEventListenerBase<AppStop>("job-mgr-stop") {
            @Override
            public void on(AppStop event) {
                jobs.get(JOB_APP_SHUTDOWN).run();
            }
        });
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

    public void beforeAppStart(final Runnable runnable) {
        jobById(JOB_APP_START).addPrecedenceJob(new _Job(S.uuid(), this, new _.F0() {
            @Override
            public Object apply() throws NotAppliedException, _.Break {
                runnable.run();
                return null;
            }
        }, true));
    }

    public void afterAppStart(final Runnable runnable) {
        jobById(JOB_APP_START).addFollowingJob(new _Job(S.uuid(), this, new _.F0() {
            @Override
            public Object apply() throws NotAppliedException, _.Break {
                runnable.run();
                return null;
            }
        }, true));
    }

    public void beforeAppStop(final Runnable runnable) {
        jobById(JOB_APP_SHUTDOWN).addFollowingJob(new _Job(S.uuid(), this, new _.F0() {
            @Override
            public Object apply() throws NotAppliedException, _.Break {
                runnable.run();
                return null;
            }
        }, true));
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

    private void registerSysJobs() {
        registerSysJob(JOB_APP_START);
        registerSysJob(JOB_APP_SHUTDOWN);
    }

    private void registerSysJob(String id) {
        addJob(new _Job(id, this));
    }

}
