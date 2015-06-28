package act.job;

import act.app.App;
import act.app.AppServiceBase;
import act.app.AppThreadFactory;
import act.app.event.AppEvent;
import act.app.event.AppEventHandlerBase;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class AppJobManager extends AppServiceBase<AppJobManager> {

    public static final String JOB_APP_START = "__act_app_started";
    public static final String JOB_APP_SHUTDOWN = "__act_app_shutdown";

    private ScheduledThreadPoolExecutor executor;
    private Map<String, _Job> jobs = C.newMap();

    public AppJobManager(App app) {
        super(app);
        initExecutor(app);
        registerSysJobs();
        app.eventManager().on(AppEvent.START, new AppEventHandlerBase("job-mgr-start") {
            @Override
            public void onEvent() {
                jobs.get(JOB_APP_START).run();
            }
        }).on(AppEvent.STOP, new AppEventHandlerBase("job-mgr-stop") {
            @Override
            public void onEvent() {
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

    public void now(Runnable runnable) {
        executor().submit(runnable);
    }

    public void beforeAppStart(final Runnable runnable) {
        jobById(JOB_APP_START).addPrecedenceJob(new _Job(S.uuid(), this, new _.F0() {
            @Override
            public Object apply() throws NotAppliedException, _.Break {
                runnable.run();
                return null;
            }
        }));
    }

    public void afterAppStart(final Runnable runnable) {
        jobById(JOB_APP_START).addFollowingJob(new _Job(S.uuid(), this, new _.F0() {
            @Override
            public Object apply() throws NotAppliedException, _.Break {
                runnable.run();
                return null;
            }
        }));
    }

    public void beforeAppStop(final Runnable runnable) {
        jobById(JOB_APP_SHUTDOWN).addFollowingJob(new _Job(S.uuid(), this, new _.F0() {
            @Override
            public Object apply() throws NotAppliedException, _.Break {
                runnable.run();
                return null;
            }
        }));
    }

    public <T> Future<T> now(Callable<T> callable) {
        return executor().submit(callable);
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
