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
import act.app.event.AppEventId;
import act.job.bytecode.ReflectedJobInvoker;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobMethodMetaInfo;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.List;

public class JobAnnotationProcessor extends AppHolderBase<JobAnnotationProcessor> {

    private static final Logger LOGGER = LogManager.get(JobAnnotationProcessor.class);

    private AppJobManager manager;

    public JobAnnotationProcessor(App app) {
        super(app);
        manager = app.jobManager();
    }

    public void register(final JobMethodMetaInfo method, final Class<? extends Annotation> anno, final Object v) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("register job[%s] on anno[%s] with arg[%s]", method, anno, v);
        }
        if (isAbstract(method)) {
            app().jobManager().on(AppEventId.SINGLETON_PROVISIONED, new Runnable() {
                @Override
                public void run() {
                    List<JobMethodMetaInfo> list = method.extendedJobMethodMetaInfoList(app());
                    for (JobMethodMetaInfo subMethodInfo : list) {
                        register(subMethodInfo, anno, v);
                    }
                }
            });
            return;
        }
        _Job job = createMethodJob(method);
        if (Cron.class.isAssignableFrom(anno)) {
            registerCron(job, evaluateExpression(v.toString(), anno));
        } else if (AlongWith.class.isAssignableFrom(anno)) {
            registerAlongWith(job, v.toString());
        } else if (Every.class.isAssignableFrom(anno)) {
            registerEvery(job, evaluateExpression(v.toString(), anno));
        } else if (FixedDelay.class.isAssignableFrom(anno)) {
            registerFixedDelay(job, evaluateExpression(v.toString(), anno));
        } else if (InvokeAfter.class.isAssignableFrom(anno)) {
            registerInvokeAfter(job, v.toString());
        } else if (InvokeBefore.class.isAssignableFrom(anno)) {
            registerInvokeBefore(job, v.toString());
        } else if (OnAppStart.class.isAssignableFrom(anno)) {
            boolean async = null == v ? false : (Boolean)v;
            registerOnAppStart(job, async);
        } else if (OnAppStop.class.isAssignableFrom(anno)) {
            boolean async = null == v ? false : (Boolean) v;
            registerOnAppStop(job, async);
        } else if (OnAppEvent.class.isAssignableFrom(anno)) {
            boolean async = false;
            AppEventId appEventId;
            if (v instanceof $.T2) {
                $.T2<AppEventId, Boolean> t2 = $.cast(v);
                appEventId = t2._1;
                async = t2._2;
            } else {
                appEventId = $.cast(v);
            }
            registerOnAppEvent(job, appEventId, async);
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

    private void registerCron(_Job job, String expression) {
        JobTrigger.cron(expression).register(job, manager);
    }

    private void registerAlongWith(_Job job, String targetJobId) {
        JobTrigger.alongWith(targetJobId).register(job, manager);
    }

    private void registerEvery(_Job job, String expression) {
        JobTrigger.every(expression).register(job, manager);
    }

    private void registerFixedDelay(_Job job, String expression) {
        JobTrigger.fixedDelay(expression).register(job, manager);
    }

    private void registerInvokeAfter(_Job job, String targetJobId) {
        JobTrigger.after(targetJobId).register(job, manager);
    }

    private void registerInvokeBefore(_Job job, String targetJobId) {
        JobTrigger.before(targetJobId).register(job, manager);
    }

    private void registerOnAppStart(_Job job, boolean async) {
        JobTrigger.onAppStart(async).register(job, manager);
    }

    private void registerOnAppStop(_Job job, boolean async) {
        JobTrigger.onAppStop(async).register(job, manager);
    }

    private void registerOnAppEvent(_Job job, AppEventId appEventId, boolean async) {
        JobTrigger.onAppEvent(appEventId, async).register(job, manager);
    }

    private boolean isAbstract(JobMethodMetaInfo method) {
        JobClassMetaInfo classMetaInfo = method.classInfo();
        return (classMetaInfo.isAbstract());
    }
    
    private _Job createMethodJob(JobMethodMetaInfo method) {
        String id = method.id();
        return new _Job(id, app().jobManager(), new ReflectedJobInvoker<>(method, app()), false);
    }
}
