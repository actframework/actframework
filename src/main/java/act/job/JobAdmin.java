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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import act.app.ActionContext;
import act.cli.*;
import act.cli.meta.CommandMethodMetaInfo;
import act.util.CsvView;
import act.util.JsonView;
import act.util.*;
import act.ws.*;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.ResponseContentType;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.inject.Inject;

/**
 * Provide admin service to act {@link JobManager}
 */
@SuppressWarnings("unused")
public class JobAdmin {

    private static Comparator<Job> _UNIQ_JOB_FILTER = new Comparator<Job>() {
        @Override
        public int compare(Job o1, Job o2) {
            return o1.id().compareTo(o2.id());
        }
    };

    /**
     * List all jobs in the job manager
     * @return a list of {@link Job jobs}
     */
    @Command(value = "act.job.list,act.job,act.jobs", help = "List jobs")
    @PropertySpec(Job.BRIEF_VIEW)
    @TableView
    public List<Job> listJobs(@Optional(lead = "-q", help = "search string") final String q, JobManager jobManager) {
        C.List<Job> jobs = jobManager.jobs().append(jobManager.virtualJobs()).unique(_UNIQ_JOB_FILTER);
        if (S.notBlank(q)) {
            jobs = jobs.filter(new $.Predicate<Job>() {
                @Override
                public boolean test(Job job) {
                    String jobStr = job.toString();
                    return jobStr.contains(q) || jobStr.matches(q);
                }
            });
        }
        return jobs;
    }

    @Command(value = "act.job.show", help = "Show job details")
    @JsonView
    @PropertySpec(Job.DETAIL_VIEW)
    public Job getJob(@Required("specify job id") final String id, JobManager jobManager) {
        return jobManager.jobById(id);
    }

    @Command(value = "act.job.progress", help = "Show job progress")
    public int getJobProgress(@Required("specify job id") final String id, JobManager jobManager) {
        Job job = jobManager.jobById(id);
        return null == job ? -1 : job.getProgressInPercent();
    }

    @Command(name = "act.job.cancel", help = "Cancel a job")
    public void cancel(@Required("specify job id") String id, JobManager jobManager) {
        jobManager.cancel(id);
    }

    @Command(value = "act.job.scheduler", help = "Show Job manager scheduler status")
    public String getSchedulerStatus(JobManager jobManager) {
        ScheduledThreadPoolExecutor executor = jobManager.executor();
        JSONObject json = new JSONObject();
        json.put("is terminating", executor.isTerminating());
        json.put("is terminated", executor.isTerminated());
        json.put("is shutdown", executor.isShutdown());
        json.put("# of runnable in the queue", executor.getQueue().size());
        json.put("active count", executor.getActiveCount());
        json.put("# of completed tasks", executor.getActiveCount());
        json.put("core pool size", executor.getCorePoolSize());
        json.put("pool size", executor.getPoolSize());
        return json.toJSONString();
    }

    @GetAction("jobs/{jobId}/result")
    @Command(value = "act.job.result", help = "Retrieve async job result")
    public Object getAsyncResult(@Required("specify job id") String jobId, JobManager jobManager, ActContext context) {
        Object result = jobManager.cachedResult(jobId);
        if (null == result) {
            return null;
        }
        Object meta = jobManager.cachedMeta(jobId);
        if (null == meta) {
            return result;
        }
        if (meta instanceof CommandMethodMetaInfo) {
            CommandMethodMetaInfo methodMetaInfo = $.cast(meta);
            PropertySpec.MetaInfo filter = methodMetaInfo.propertySpec();
            methodMetaInfo.view().print(result, filter, (CliContext) context);
            return null;
        }
        if (meta instanceof Method) {
            Method method = $.cast(meta);
            PropertySpec spec = method.getAnnotation(PropertySpec.class);
            if (null != spec) {
                PropertySpec.MetaInfo metaInfo = new PropertySpec.MetaInfo();
                for (String s : spec.value()) {
                    metaInfo.onValue(s);
                }
                for (String s : spec.http()) {
                    metaInfo.onHttp(s);
                }
                PropertySpec.currentSpec.set(metaInfo);
            }
            if (isAnnotationPresented(JsonView.class, method)) {
                context.accept(H.Format.JSON);
            } else if (isAnnotationPresented(CsvView.class, method)) {
                context.accept(H.Format.CSS);
            } else {
                ResponseContentType rct = getAnnotation(ResponseContentType.class, method);
                if (null != rct) {
                    context.accept(rct.value().format());
                }
            }
            String attachmentName = (String) jobManager.cachedPayload(jobId, "attachmentName");
            if (null != attachmentName && context instanceof ActionContext) {
                ((ActionContext) context).downloadFileName(attachmentName);
            }
            return result;
        }
        throw new UnexpectedException("Oops! how come? The cached async handler meta is type of " + meta.getClass());
    }

    private boolean isAnnotationPresented(Class<? extends Annotation> annotationClass, Method method) {
        return null != getAnnotation(annotationClass, method);
    }

    private <T extends Annotation> T getAnnotation(Class<T> annotationClass, Method method) {
        T anno = ReflectedInvokerHelper.getAnnotation(annotationClass, method);
        if (null != anno) {
            return anno;
        }
        return method.getClass().getAnnotation(annotationClass);
    }

    @GetAction("jobs/{id}/progress")
    public ProgressGauge jobProgress(String id, JobManager jobManager) {
        Job job = jobManager.jobById(id);
        if (null == job) {
            return SimpleProgressGauge.NULL;
        }
        ProgressGauge gauge = job.progress();
        return null == gauge ? SimpleProgressGauge.NULL : gauge;
    }

    @WsEndpoint("/~/ws/jobs/{jobId}/progress")
    public static class WsProgress implements WebSocketConnectionListener {
        @Inject
        private WebSocketConnectionManager connectionManager;
        @Inject
        private JobManager jobManager;

        @Override
        public void onConnect(final WebSocketContext context) {
            String jobId = context.actionContext().paramVal("jobId");
            Job job = null;
            if (null != jobId) {
                job = jobManager.jobById(jobId);
            }
            if (null == job) {
                jobManager.delay(new Runnable() {
                    @Override
                    public void run() {
                        context.connection().close();
                    }
                }, 10, MILLISECONDS);
                return;
            }
            String tag = SimpleProgressGauge.wsJobProgressTag(jobId);
            connectionManager.subscribe(context.session(), tag);
            job.progress().addListener(new ProgressGauge.Listener() {
                @Override
                public void onUpdate(ProgressGauge progressGauge) {
                    if (progressGauge.isDone()) {
                        jobManager.delay(new Runnable() {
                            @Override
                            public void run() {
                                context.connection().close();
                            }
                        }, 10, MILLISECONDS);
                    }
                }
            });
        }

        @Override
        public void onClose(WebSocketContext context) {
        }
    }

}
