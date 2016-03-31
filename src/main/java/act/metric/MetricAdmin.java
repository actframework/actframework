package act.metric;

import act.Act;
import act.cli.Command;
import act.cli.Optional;
import act.util.PropertySpec;
import org.osgl.util.C;

import java.util.List;

/**
 * Console app to access actframework metric data
 */
public class MetricAdmin {

    @Command(name = "act.metric.counter.list", help = "list all counters")
    @PropertySpec("name,count")
    public List<MetricInfo> getCounters(
            @Optional("specify maximum items returned") Integer max
    ) {
        return chop(Act.metricPlugin().metricStore().counters(), max);
    }

    @Command(name = "act.metric.timer.list", help = "list all timers")
    @PropertySpec("name,ms,count,msAvg")
    public List<MetricInfo> getTimers(
            @Optional("specify maximum items returned") Integer max
    ) {
        return chop(Act.metricPlugin().metricStore().timers(), max);
    }

    private List<MetricInfo> chop(List<MetricInfo> list, Integer max) {
        if (null == max) {
            return list;
        }
        return C.list(list).take(max);
    }

}
