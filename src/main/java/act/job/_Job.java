package act.job;

import act.Act;
import act.app.App;
import act.app.event.AppEventId;
import act.event.AppEventListenerBase;
import act.route.DuplicateRouteMappingException;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.ConfigurationException;
import org.osgl.exception.NotAppliedException;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Collection;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;

class _Job extends DestroyableBase implements Runnable {

    private static final Logger logger = LogManager.get(_Job.class);

    static final String BRIEF_VIEW = "id,oneTime,executed,trigger";
    static final String DETAIL_VIEW = "id,oneTime,executed,trigger,worker";

    private static final C.Set<Class<? extends UnexpectedException>> FATAL_EXCEPTIONS = C.set(
            DuplicateRouteMappingException.class,
            ConfigurationException.class
    );

    private String id;
    private boolean oneTime;
    private boolean executed;
    private AppJobManager manager;
    private JobTrigger trigger;
    private $.Func0<?> worker;
    private List<_Job> parallelJobs = C.newList();
    private List<_Job> followingJobs = C.newList();
    private List<_Job> precedenceJobs = C.newList();

    _Job(AppJobManager manager) {
        this(Act.cuid(), manager);
    }

    _Job(String id, AppJobManager manager) {
        this(id, manager, null);
    }

    _Job(String id, AppJobManager manager, $.Func0<?> worker) {
        this(id, manager, worker, true);
    }

    _Job(String id, AppJobManager manager, $.Func0<?> worker, boolean oneTime) {
        this.id = id;
        this.manager = $.NPE(manager);
        this.worker = worker;
        this.oneTime = oneTime;
    }

    @Override
    protected void releaseResources() {
        worker = null;
        manager = null;
        parallelJobs.clear();
        followingJobs.clear();
        precedenceJobs.clear();
        super.releaseResources();
    }

    protected String brief() {
        return S.fmt("job[%s]\none time job?%s\ntrigger:%s", id, oneTime, trigger);
    }

    @Override
    public String toString() {
        StringBuilder sb = S.builder(brief());
        printSubJobs(parallelJobs, "parallel jobs", sb);
        printSubJobs(followingJobs, "following jobs", sb);
        printSubJobs(precedenceJobs, "precedence jobs", sb);
        return sb.toString();
    }

    private static void printSubJobs(Collection<? extends _Job> subJobs, String label, StringBuilder sb) {
        if (null != subJobs && !subJobs.isEmpty()) {
            sb.append("\n").append(label);
            for (_Job job : subJobs) {
                sb.append("\n\t").append(job.brief());
            }
        }
    }

    _Job setOneTime() {
        oneTime = true;
        return this;
    }

    boolean isOneTime() {
        return oneTime;
    }

    boolean done() {
        return executed && oneTime;
    }

    final String id() {
        return id;
    }

    final void trigger(JobTrigger trigger) {
        E.NPE(trigger);
        this.trigger = trigger;
    }

    final _Job addParallelJob(_Job thatJob) {
        if (done()) {
            manager.now(thatJob);
            return this;
        }
        parallelJobs.add(thatJob);
        if (isOneTime()) {
            thatJob.setOneTime();
        }
        return this;
    }

    final _Job addFollowingJob(_Job thatJob) {
        if (done()) {
            thatJob.run();
            return this;
        }
        followingJobs.add(thatJob);
        if (isOneTime()) {
            thatJob.setOneTime();
        }
        return this;
    }

    final _Job addPrecedenceJob(_Job thatJob) {
        if (done()) {
            thatJob.run();
            return this;
        }
        precedenceJobs.add(thatJob);
        if (isOneTime()) {
            thatJob.setOneTime();
        }
        return this;
    }

    @Override
    public void run() {
        invokeParallelJobs();
        runPrecedenceJobs();
        try {
            doJob();
        } catch (RuntimeException e) {
            if (FATAL_EXCEPTIONS.contains(e.getClass())) {
                Act.shutdownApp(App.instance());
                destroy();
                if (App.instance().isMainThread()) {
                    throw e;
                } else {
                    logger.fatal(e, "Fatal error executing job %s", id());
                }
                return;
            }
            // TODO inject Job Exception Handling mechanism here
            logger.warn(e, "error executing job %s", id());
        } finally {
            if (!isDestroyed()) {
                executed = true;
                if (isOneTime()) {
                    App app = App.instance();
                    if (AppEventId.POST_START == app.currentState()) {
                        manager.removeJob(this);
                    } else {
                        app.eventBus().bind(AppEventId.POST_START, new AppEventListenerBase() {
                            @Override
                            public void on(EventObject event) throws Exception {
                                manager.removeJob(_Job.this);
                            }
                        });
                    }
                }
            }
        }
        runFollowingJobs();
    }

    protected void doJob() {
        try {
            if (null != worker) {
                worker.apply();
            }
        } finally {
            scheduleNextInvocation();
        }
    }

    private void runPrecedenceJobs() {
        for (_Job precedence : precedenceJobs) {
            precedence.run();
        }
    }

    private void runFollowingJobs() {
        for (_Job post : followingJobs) {
            post.run();
        }
    }

    private void invokeParallelJobs() {
        for (_Job alongWith : parallelJobs) {
            manager.now(alongWith);
        }
    }

    protected final AppJobManager manager() {
        return manager;
    }

    protected void scheduleNextInvocation() {
        if (null != trigger) trigger.scheduleFollowingCalls(manager(), this);
    }

    private static _Job of(String jobId, final Runnable runnable, AppJobManager manager, boolean oneTime) {
        return new _Job(jobId, manager, new $.F0() {
            @Override
            public Object apply() throws NotAppliedException, $.Break {
                runnable.run();
                return null;
            }
        }, oneTime);
    }

    private static _Job of(final Runnable runnable, AppJobManager manager, boolean oneTime) {
        return of(S.uuid(), runnable, manager, oneTime);
    }

    static _Job once(final Runnable runnable, AppJobManager manager) {
        return of(runnable, manager, true);
    }

    static _Job once(String jobId, final Runnable runnable, AppJobManager manager) {
        return of(jobId, runnable, manager, true);
    }

    static _Job multipleTimes(final Runnable runnable, AppJobManager manager) {
        return of(runnable, manager, false);
    }

    static _Job multipleTimes(String jobId, final Runnable runnable, AppJobManager manager) {
        return of(jobId, runnable, manager, false);
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
