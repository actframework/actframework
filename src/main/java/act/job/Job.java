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

import static act.util.SimpleProgressGauge.wsJobProgressTag;
import static org.osgl.$.F0;

import act.Act;
import act.app.App;
import act.app.event.SysEventId;
import act.event.SysEventListenerBase;
import act.job.bytecode.ReflectedJobInvoker;
import act.route.DuplicateRouteMappingException;
import act.util.*;
import act.util.ProgressGauge;
import act.ws.WebSocketConnectionManager;
import org.osgl.$;
import org.osgl.exception.*;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * A `Job` is a piece of logic that can be run/scheduled in ActFramework
 */
public class Job extends DestroyableBase implements Runnable {

    private static final Logger LOGGER = LogManager.get(Job.class);

    private static class LockableJobList {
        boolean iterating;
        List<Job> jobList;
        Job parent;
        boolean sysJob;

        LockableJobList(Job parent) {
            this.jobList = new ArrayList<>();
            this.parent = parent;
            this.sysJob = null != parent && parent.sysJob;
        }

        void markAsSysJob() {
            this.sysJob = true;
            for (Job job : jobList) {
                job.markAsSysJob();
            }
        }

        synchronized void clear() {
            jobList.clear();
        }

        synchronized Job add(Job thatJob) {
            if (!parent.isOneTime()) {
                thatJob.setNonOneTime();
            }
            if (parent.done() || iterating) {
                parent.manager.now(thatJob, sysJob);
                return parent;
            }
            jobList.add(thatJob);
            if (sysJob) {
                thatJob.markAsSysJob();
            }
            // Note we can't do this otherwise route registration
            // process will be broken
            // - Collections.sort(jobList, Sorter.COMPARATOR);
            return parent;
        }

        synchronized void runSubJobs() {
            runSubJobs(false);
        }

        synchronized void runSubJobs(boolean async) {
            if (jobList.isEmpty()) {
                return;
            }
            final JobManager jobManager = async ? Act.jobManager() : null;
            iterating = true;
            try {
                for (final Job subJob : jobList) {
                    if (null != jobManager) {
                        jobManager.now(new Runnable() {
                            @Override
                            public void run() {
                                subJob.run();
                            }
                        }, subJob.sysJob);
                    } else {
                        subJob.run();
                        if (Act.isDev() && subJob.app.hasBlockIssue()) {
                            break;
                        }
                    }
                }
            } finally {
                iterating = false;
            }
        }
    }

    static final String BRIEF_VIEW = "id,oneTime,executed,trigger";
    static final String DETAIL_VIEW = "id,oneTime,executed,trigger,worker";

    private static final C.Set<Class<? extends UnexpectedException>> FATAL_EXCEPTIONS = C.set(
            DuplicateRouteMappingException.class,
            ConfigurationException.class
    );

    private final String id;
    private final String jobProgressTag;
    private App app;
    private boolean oneTime = true;
    private boolean executed;
    private JobManager manager;
    private JobTrigger trigger;
    private $.Func0<?> worker;
    Object callableResult;
    Exception callableException;
    private Method method;
    private boolean sysJob;
    // progress percentage
    private ProgressGauge progress = new SimpleProgressGauge();
    private LockableJobList parallelJobs = new LockableJobList(this);
    private LockableJobList followingJobs = new LockableJobList(this);
    private LockableJobList precedenceJobs = new LockableJobList(this);

    private Job(String id) {
        this.id = id;
        this.jobProgressTag = wsJobProgressTag(id);
        if (JobManager.isSysJob(this)) {
            markAsSysJob();
        }
    }

    Job(String id, JobManager manager) {
        this(id, manager, ($.Func0<?>)null);
    }

    Job(String id, boolean sysJob, JobManager manager) {
        this(id, manager, ($.Func0<?>) null);
        if (sysJob) {
            markAsSysJob();
        }
    }

    Job(String id, JobManager manager, final Callable<?> callable) {
        this.id = id;
        this.manager = $.requireNotNull(manager);
        this.app = manager.app();
        this.jobProgressTag = wsJobProgressTag(id);
        this.manager.addJob(this);
        this.worker = new F0() {
            @Override
            public Object apply() throws NotAppliedException, $.Break {
                try {
                    callableResult = callable.call();
                } catch (Exception e) {
                    callableException = e;
                }
                return null;
            }
        };
        if (JobManager.isSysJob(this)) {
            markAsSysJob();
        }
    }

    Job(String id, JobManager manager, $.Func0<?> worker) {
        this(id, manager, worker, true);
    }

    Job(String id, JobManager manager, $.Func0<?> worker, boolean oneTime) {
        this.id = id;
        this.manager = $.NPE(manager);
        this.worker = worker;
        this.oneTime = oneTime;
        this.app = manager.app();
        this.jobProgressTag = wsJobProgressTag(id);
        this.manager.addJob(this);
        if (worker instanceof ReflectedJobInvoker) {
            this.method = ((ReflectedJobInvoker) worker).method();
        }
        if (JobManager.isSysJob(this)) {
            markAsSysJob();
        }
    }

    Job(String id, JobManager manager, $.Function<ProgressGauge, ?> worker) {
        this(id, manager, worker, true);
    }

    Job(String id, JobManager manager, $.Function<ProgressGauge, ?> worker, boolean oneTime) {
        this.id = id;
        this.manager = $.requireNotNull(manager);
        $.F1<ProgressGauge, ?> f1 = $.f1(worker);
        this.worker = f1.curry(progress);
        this.oneTime = oneTime;
        this.app = manager.app();
        this.jobProgressTag = wsJobProgressTag(id);
        this.manager.addJob(this);
        if (JobManager.isSysJob(this)) {
            markAsSysJob();
        }
    }

    public void setProgressGauge(ProgressGauge progressGauge) {
        progress = progressGauge;
        progress.setId(getId());
        progress.addListener(new ProgressGauge.Listener() {
            @Override
            public void onUpdate(ProgressGauge progressGauge) {
                Map<String, ProgressGauge> payload = C.Map("act_job_progress", progressGauge);
                app.getInstance(WebSocketConnectionManager.class).sendJsonToTagged(payload, jobProgressTag);
            }
        });
    }

    public ProgressGauge progress() {
        return progress;
    }

    public int getProgressInPercent() {
        return progress.currentProgressPercent();
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
        return S.concat("job[", id, "]\none time job:", S.string(oneTime), "\ntrigger:", S.string(trigger));
    }

    void markAsSysJob() {
        this.sysJob = true;
        this.parallelJobs.markAsSysJob();
        this.followingJobs.markAsSysJob();
        this.precedenceJobs.markAsSysJob();
    }

    boolean isSysJob() {
        return sysJob;
    }

    @Override
    public String toString() {
        S.Buffer sb = S.buffer(brief());
        printSubJobs(parallelJobs.jobList, "parallel jobs", sb);
        printSubJobs(followingJobs.jobList, "following jobs", sb);
        printSubJobs(precedenceJobs.jobList, "precedence jobs", sb);
        return sb.toString();
    }

    public Method method() {
        return method;
    }

    private static void printSubJobs(Collection<? extends Job> subJobs, String label, S.Buffer sb) {
        if (null != subJobs && !subJobs.isEmpty()) {
            sb.append("\n").append(label);
            for (Job job : subJobs) {
                sb.append("\n\t").append(job.brief());
            }
        }
    }

    void setNonOneTime() {
        oneTime = false;
    }

    boolean done() {
        return executed && oneTime;
    }

    public final String id() {
        return id;
    }

    final void trigger(JobTrigger trigger) {
        E.NPE(trigger);
        this.trigger = trigger;
    }

    final Job addParallelJob(Job thatJob) {
        return parallelJobs.add(thatJob);
    }

    final Job addFollowingJob(Job thatJob) {
        return followingJobs.add(thatJob);
    }

    final Job addPrecedenceJob(Job thatJob) {
        return precedenceJobs.add(thatJob);
    }

    @Override
    public void run() {
        invokeParallelJobs();
        runPrecedenceJobs();
        try {
            if (Act.isDev() && !Act.conf().hotReloadDisabled() && app.isStarted() && !sysJob) {
                if (app.checkUpdates(false)) {
                    // app reloaded
                    return;
                }
            }
            doJob();
            progress.markAsDone();
        } catch (Throwable t) {
            boolean isFatal = FATAL_EXCEPTIONS.contains(t.getClass()) || !Exception.class.isInstance(t);
            Throwable cause = t;
            if (!isFatal) {
                cause = t.getCause();
                while (null != cause) {
                    isFatal = FATAL_EXCEPTIONS.contains(cause.getClass());
                    if (isFatal) {
                        break;
                    }
                    cause = cause.getCause();
                }
            }
            String msg = t.getLocalizedMessage();
            if (S.blank(msg)) {
                msg = S.fmt("Unexpected exception encountered: %s, please check console log", t.getClass().getSimpleName());
            }
            progress.fail(msg);
            if (isFatal) {
                app.handleBlockIssue(t);
                destroy();
                if (App.instance().isMainThread()) {
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw E.unexpected(t);
                }
                return;
            }

            JobExceptionListenerManager manager = Act.jobManager().exceptionListenerManager();
            manager.handleJobException(id(), (Exception) t);
            if (app.isMainThread()) {
                throw t;
            }
        } finally {
            if (!isDestroyed()) {
                executed = true;
                if (isOneTime()) {
                    App app = App.instance();
                    if (app.isStarted()) {
                        manager.removeJob(this);
                    } else {
                        app.eventBus().bind(SysEventId.POST_START, new SysEventListenerBase() {
                            @Override
                            public void on(EventObject event) throws Exception {
                                manager.removeJob(Job.this);
                            }
                        });
                    }
                }
                progress.destroy();
            }
        }
        runFollowingJobs();
    }

    protected void _before() {
    }

    protected void doJob(){
        JobContext.init(id());
        ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
        if (ctxClassLoader != app.classLoader()) {
            Thread.currentThread().setContextClassLoader(app.classLoader());
        }
        try {
            _before();
            if (null != worker) {
                worker.apply();
            }
        } finally {
            JobContext.clear();
            scheduleNextInvocation();
            _finally();
            if (ctxClassLoader != app.classLoader()) {
                Thread.currentThread().setContextClassLoader(ctxClassLoader);
            }
        }
    }

    protected void _finally() {}

    protected void cancel() {
        manager.cancel(id());
    }

    private void runPrecedenceJobs() {
        precedenceJobs.runSubJobs();
    }

    private void runFollowingJobs() {
        followingJobs.runSubJobs();
    }

    private void invokeParallelJobs() {
        parallelJobs.runSubJobs(true);
    }

    protected final JobManager manager() {
        return manager;
    }

    protected void scheduleNextInvocation() {
        if (null != trigger) trigger.scheduleFollowingCalls(manager(), this);
    }

    private static Job of(String jobId, final Runnable runnable, JobManager manager, boolean oneTime) {
        return new Job(jobId, manager, new F0() {
            @Override
            public Object apply() throws NotAppliedException, $.Break {
                runnable.run();
                return null;
            }
        }, oneTime);
    }

    private static Job of(final Runnable runnable, JobManager manager, boolean oneTime) {
        return of(Act.cuid(), runnable, manager, oneTime);
    }

    static Job once(final Runnable runnable, JobManager manager) {
        return of(runnable, manager, true);
    }

    static Job once(String jobId, final Runnable runnable, JobManager manager) {
        return of(jobId, runnable, manager, true);
    }

    static Job multipleTimes(final Runnable runnable, JobManager manager) {
        return of(runnable, manager, false);
    }

    static Job multipleTimes(String jobId, final Runnable runnable, JobManager manager) {
        return of(jobId, runnable, manager, false);
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    // ---- Java bean accessors
    public String getId() {
        return id;
    }

    public boolean isExecuted() {
        return executed;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public JobTrigger trigger() {
        return trigger;
    }

    static Job virtualJob(String jobId, JobManager manager) {
        Job job = new Job(jobId);
        job.manager = manager;
        return job;
    }
}
