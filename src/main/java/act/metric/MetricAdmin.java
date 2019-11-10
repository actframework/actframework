package act.metric;

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

import act.Act;
import act.cli.CliContext;
import act.cli.Command;
import act.cli.Optional;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.util.C;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Console app to access actframework metric data
 */
public class MetricAdmin {

    @Command(name = "act.metric.sync")
    public void updateMetricDataSync(
            @Optional("pause sync") boolean pause,
            CliContext context
    ) {
        Act.metricPlugin().enableDataSync(!pause);
        context.println(pause ? "metric data sync paused." : "metric data sync enabled");
    }

    @Command(name = "act.metric.counter.list,act.metric.counter,act.counter,act.metric.counters,act.counters.", help = "show metric counters")
    @PropertySpec("name,count")
    public Object getCounters(
            @Optional("specify maximum items returned") Integer limit,
            @Optional("display in tree view") boolean tree,
            @Optional("specify depth of levels") Integer depth,
            @Optional("specify search string") String q,
            @Optional("including classloading metric") boolean classLoading
    ) {
        List<MetricInfo> list = Act.metricPlugin().metricStore().timers();
        if (!classLoading) {
            list = withoutClassLoading(list);
        }
        return process(list, limit, q, tree, depth, MetricInfo.Comparator.COUNTER, MetricInfoTree.COUNTER);
    }

    @Command(name = "act.metric.timer.list,act.metric.timer,act.metric,act.timer,act.metrics,act.timers", help = "show metric timers")
    @PropertySpec("name,accumulated,count,avg")
    public Object getTimers(
            @Optional("specify maximum items returned") Integer limit,
            @Optional("display in tree view") boolean tree,
            @Optional("specify depth of levels") Integer depth,
            @Optional("specify search string") String q,
            @Optional("including classloading metric") boolean classLoading
    ) {
        List<MetricInfo> list = Act.metricPlugin().metricStore().timers();
        if (!classLoading) {
            list = withoutClassLoading(list);
        }
        return process(list, limit, q, tree, depth, MetricInfo.Comparator.TIMER, MetricInfoTree.TIMER);
    }

    private List<MetricInfo> withoutClassLoading(List<MetricInfo> list) {
        return C.list(list).remove(new $.Predicate<MetricInfo>() {
            @Override
            public boolean test(MetricInfo metricInfo) {
                return metricInfo.getName().startsWith(MetricInfo.CLASS_LOADING);
            }
        });
    }

    @Command(name = "act.metric.clear", help = "clear existing metric data")
    public void clearMetricData() {
        Act.metricPlugin().metricStore().clear();
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
