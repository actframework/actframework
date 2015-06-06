package act.job;

import act.app.App;
import act.app.AppServiceBase;
import act.app.AppThreadFactory;
import act.app.event.AppEvent;
import act.app.event.AppEventListener;
import org.osgl.util.C;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class AppJobManager extends AppServiceBase<AppJobManager> implements AppEventListener {

    public static final String JOB_APP_START = "__act_app_started";
    public static final String JOB_APP_SHUTDOWN = "__act_app_shutdown";

    private ScheduledThreadPoolExecutor executor;
    private Map<String, _Job> jobs = C.newMap();

    public AppJobManager(App app) {
        super(app);
        initExecutor(app);
        registerSysJobs();
        app.eventManager().register(this);
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

    @Override
    public void handleAppEvent(AppEvent event) {
        switch (event) {
            case START:
                jobs.get(JOB_APP_START).run();
                break;
            case STOP:
                jobs.get(JOB_APP_SHUTDOWN).run();
                break;
        }
    }

    public void start(Runnable runnable) {
        executor().submit(runnable);
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
