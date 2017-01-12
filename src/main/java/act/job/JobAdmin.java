package act.job;

import act.cli.*;
import act.util.PropertySpec;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Provide admin service to act {@link AppJobManager}
 */
@SuppressWarnings("unused")
public class JobAdmin {

    /**
     * List all jobs in the job manager
     * @return a list of {@link _Job jobs}
     */
    @Command(value = "act.job.list", help = "List jobs")
    @PropertySpec(_Job.BRIEF_VIEW)
    @TableView
    public List<_Job> listJobs(@Optional(lead = "-q") final String q, AppJobManager jobManager) {
        C.List<_Job> jobs = jobManager.jobs().append(jobManager.virtualJobs());
        if (S.notBlank(q)) {
            jobs = jobs.filter(new $.Predicate<_Job>() {
                @Override
                public boolean test(_Job job) {
                    return job.toString().contains(q);
                }
            });
        }
        return jobs;
    }

    @Command(value = "act.job.show", help = "Show job details")
    @JsonView
    @PropertySpec(_Job.DETAIL_VIEW)
    public _Job getJob(@Required("specify job id") final String id, AppJobManager jobManager) {
        return jobManager.jobById(id);
    }

    @Command(name = "act.job.cancel", help = "Cancel a job")
    public void cancel(@Required("specify job id") String id, AppJobManager jobManager) {
        jobManager.cancel(id);
    }

    @Command(value = "act.job.scheduler", help = "Show Job manager scheduler status")
    public String getSchedulerStatus(AppJobManager jobManager) {
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
}
