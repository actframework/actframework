package act.job;

import act.app.App;
import act.cli.Command;
import act.cli.HelpMsg;
import act.cli.Optional;
import act.cli.Required;
import act.util.DataView;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Provide admin service to act {@link AppJobManager}
 */
public class JobAdmin {

    /**
     * List all jobs in the job manager
     * @return a list of {@link _Job jobs}
     */
    @Command("act.job.list")
    @HelpMsg("List jobs")
    @DataView(_Job.LIST_VIEW)
    public List<_Job> listJobs(@Optional("-q") final String q, App app) {
        AppJobManager jobManager = app.jobManager();
        C.List<_Job> jobs = jobManager.jobs();
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

    @Command("act.job.show")
    @HelpMsg("Show job")
    public _Job getJob(@Required(value = "-i,--id", help = "specify the query string") final String id, App app) {
        AppJobManager jobManager = app.jobManager();
        return jobManager.jobById(id);
    }

    @Command("act.job.scheduler")
    @HelpMsg("Show Job manager scheduler status")
    public String getSchedulerStatus(App app) {
        AppJobManager jobManager = app.jobManager();
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
