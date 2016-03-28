package act.metric;

import act.Act;
import act.cli.Command;
import act.util.PropertySpec;

import java.util.List;

/**
 * Console app to access actframework metric data
 */
public class MetricAdmin {

    @Command(name = "act.metric.counter.list", help = "list all counters")
    @PropertySpec("name,count")
    public List<MetricInfo> getCounters() {
        return Act.metricPlugin().metricStore().counters();
    }

    @Command(name = "act.metric.timer.list", help = "list all timers")
    @PropertySpec("name,ms,count,msAvg")
    public List<MetricInfo> getTimers() {
        return Act.metricPlugin().metricStore().timers();
    }

}
