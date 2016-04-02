package act.metric;

import act.Act;
import act.cli.Command;
import act.cli.Optional;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Console app to access actframework metric data
 */
public class MetricAdmin {

    @Command(name = "act.metric.counter.list", help = "list all counters")
    @PropertySpec("name,count")
    public Object getCounters(
            @Optional("specify maximum items returned") Integer limit,
            @Optional("display in tree view") boolean tree,
            @Optional("specify depth of levels") Integer depth,
            @Optional("specify search string") String q
    ) {
        List<MetricInfo> list = Act.metricPlugin().metricStore().timers();
        return process(list, limit, q, tree, depth, MetricInfo.Comparator.COUNTER, MetricInfoTree.COUNTER);
    }

    @Command(name = "act.metric.timer.list", help = "list all timers")
    @PropertySpec("name,accumulated,count,avg")
    public Object getTimers(
            @Optional("specify maximum items returned") Integer limit,
            @Optional("display in tree view") boolean tree,
            @Optional("specify depth of levels") Integer depth,
            @Optional("specify search string") String q
    ) {
        List<MetricInfo> list = Act.metricPlugin().metricStore().timers();
        return process(list, limit, q, tree, depth, MetricInfo.Comparator.TIMER, MetricInfoTree.TIMER);
    }

    private Object process(List<MetricInfo> list, Integer max, final String q,
                           boolean asTree, final Integer level, Comparator<MetricInfo> comp,
                           MetricInfoTree.NodeDecorator decorator) {
        $.Predicate<String> filter = $.F.yes();
        if (null != level) {
            filter = (new $.Predicate<String>() {
                @Override
                public boolean test(String path) {
                    String[] sa = path.split(Metric.PATH_SEPARATOR);
                    return sa.length <= level;
                }
            });
        }
        if (null != q) {
            final Pattern p = Pattern.compile(q);
            filter = filter.and(new $.Predicate<String>() {
                @Override
                public boolean test(String path) {
                    return path.contains(q) || p.matcher(path).matches();
                }
            });
        }
        final $.Predicate<String> theFilter = filter;
        if (!asTree) {
            list = C.list(list).filter(new $.Predicate<MetricInfo>() {
                @Override
                public boolean test(MetricInfo metricInfo) {
                    return theFilter.test(metricInfo.getName());
                }
            });
            if (null == max) {
                return C.list(list).sorted(comp);
            }
            return C.list(list).take(max).sorted(comp);
        } else {
            MetricInfoTree tree = new MetricInfoTree(list, theFilter);
            return tree.root(decorator);
        }
    }

}
