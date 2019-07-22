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

import act.app.App;
import act.app.AppHolderBase;
import act.app.event.SysEventId;
import act.test.FixtureLoader;
import act.job.bytecode.JobAnnoInfo;
import act.job.bytecode.ReflectedJobInvoker;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobMethodMetaInfo;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.List;

public class JobAnnotationProcessor extends AppHolderBase<JobAnnotationProcessor> {

    private static final Logger LOGGER = LogManager.get(JobAnnotationProcessor.class);

    private JobManager manager;

    public JobAnnotationProcessor(App app) {
        super(app);
        manager = app.jobManager();
    }

    public void register(final JobMethodMetaInfo method, final Class<? extends Annotation> anno, final JobAnnoInfo info) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("register job[%s] on anno[%s] with arg[%s]", method, anno, info);
        }
        if (isAbstract(method)) {
            app().jobManager().on(SysEventId.SINGLETON_PROVISIONED, "JobAnnotationProcessor:registerJobMethods:" + method, new Runnable() {
                @Override
                public void run() {
                    List<JobMethodMetaInfo> list = method.extendedJobMethodMetaInfoList(app());
                    for (JobMethodMetaInfo subMethodInfo : list) {
                        register(subMethodInfo, anno, info);
                    }
                }
            });
            return;
        }
        Job job = getOrCreateMethodJob(method, anno != FixtureLoader.class);
        String value = info.value;
        if (Cron.class.isAssignableFrom(anno)) {
            registerCron(job, evaluateExpression(value, anno));
        } else if (AlongWith.class.isAssignableFrom(anno)) {
            int delayInSeconds = info.delayInSeconds;
            registerAlongWith(job, value, delayInSeconds);
        } else if (Every.class.isAssignableFrom(anno)) {
            registerEvery(job, evaluateExpression(value, anno), info.startImmediately);
        } else if (FixedDelay.class.isAssignableFrom(anno)) {
            registerFixedDelay(job, evaluateExpression(value, anno), info.startImmediately);
        } else if (InvokeAfter.class.isAssignableFrom(anno)) {
            registerInvokeAfter(job, value);
        } else if (InvokeBefore.class.isAssignableFrom(anno)) {
            registerInvokeBefore(job, value);
        } else if (OnAppStart.class.isAssignableFrom(anno)) {
            boolean async = info.async;
            int delayInSeconds = info.delayInSeconds;
            registerOnAppStart(job, async, delayInSeconds);
        } else if (OnAppStop.class.isAssignableFrom(anno)) {
            boolean async = info.async;
            registerOnAppStop(job, async);
        } else if (OnSysEvent.class.isAssignableFrom(anno)) {
            registerOnSysEvent(job, info.sysEventId, info.async);
        } else if (FixtureLoader.class.isAssignableFrom(anno)) {
            // fixture loader job already registered;
        } else {
            throw E.unsupport("Unknown job annotation class: %s", anno.getName());
        }
    }

    private String evaluateExpression(String expression, Class<? extends Annotation> annoClass) {
        String prefix = annoClass.getSimpleName();
        if (S.eq(FixedDelay.class.getName(), prefix)) {
            prefix = "fixed-delay";
        } else {
            prefix = prefix.toLowerCase();
        }
        String ret = expression.trim();
        if (ret.startsWith(prefix)) {
            ret = (String) app().config().get(expression);
            if (S.blank(ret)) {
                throw E.invalidConfiguration("Expression configuration not found: %s", expression);
            }
        }
        return ret;
    }

    private void registerCron(Job job, String expression) {
        JobTrigger.cron(expression).register(job, manager);
    }

    private void registerAlongWith(Job job, String targetJobId, int delayInSeconds) {
        if (delayInSeconds > 0) {
            JobTrigger.delayAfter(targetJobId, delayInSeconds).register(job, manager);
        } else {
            JobTrigger.alongWith(targetJobId).register(job, manager);
        }
    }

    private void registerEvery(Job job, String expression, boolean startImmediately) {
        JobTrigger.every(expression, startImmediately).register(job, manager);
    }

    private void registerFixedDelay(Job job, String expression, boolean startImmediately) {
        JobTrigger.fixedDelay(expression, startImmediately).register(job, manager);
    }

    private void registerInvokeAfter(Job job, String targetJobId) {
        JobTrigger.after(targetJobId).register(job, manager);
    }

    private void registerInvokeBefore(Job job, String targetJobId) {
        JobTrigger.before(targetJobId).register(job, manager);
    }

    private void registerOnAppStart(Job job, boolean async, int delayInSeconds) {
        JobTrigger.onAppStart(async, delayInSeconds).register(job, manager);
    }

    private void registerOnAppStop(Job job, boolean async) {
        JobTrigger.onAppStop(async).register(job, manager);
    }

    private void registerOnSysEvent(Job job, SysEventId sysEventId, boolean async) {
        JobTrigger.onSysEvent(sysEventId, async).register(job, manager);
    }

    private boolean isAbstract(JobMethodMetaInfo method) {
        if (method.isStatic()) {
            return false;
        }
        JobClassMetaInfo classMetaInfo = method.classInfo();
        return (classMetaInfo.isAbstract());
    }

    private Job getOrCreateMethodJob(JobMethodMetaInfo method) {
        return getOrCreateMethodJob(method, true);
    }
    
    private Job getOrCreateMethodJob(JobMethodMetaInfo method, boolean oneTime) {
        String id = method.id();
        JobManager jobManager = app().jobManager();
        Job job = jobManager.jobById(id, false);
        return null == job ? new Job(id, app().jobManager(), new ReflectedJobInvoker<>(method, app()), oneTime) : job;
    }
}
