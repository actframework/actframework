package act.metric;

import org.osgl.$;

public class MetricInfo implements Comparable<MetricInfo> {

    public static final String ACTION_HANDLER = "action-handler";
    public static final String JOB_HANDLER = "job-handler";
    public static final String ROUTING = "routing";

    private String name;
    private long count;
    private Long ns;

    MetricInfo(String name, long count) {
        this.name = name;
        this.count = count;
    }

    MetricInfo(String name, long ns, long count) {
        this.name = name;
        this.ns = ns;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public long getNs() {
        return ns;
    }

    public long getMs() {
        return ns / 1000L / 1000L;
    }

    public long getSeconds() {
        return ns / 1000L / 1000L / 1000L;
    }

    public long getCount() {
        return count;
    }

    public long getMsAvg() {
        return getMs() / count;
    }

    @Override
    public int hashCode() {
        return $.hc(name, ns);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MetricInfo) {
            MetricInfo that = (MetricInfo) obj;
            return ns == that.ns & $.eq(name, that.name);
        }
        return false;
    }

    @Override
    public int compareTo(MetricInfo metricInfo) {
        long l;
        if (null == ns) {
            l = metricInfo.count - count;
        } else {
            l = metricInfo.getMsAvg() - getMsAvg();
        }
        if (l < 0) {
            return -1;
        } else if (l == 0) {
            return 0;
        } else {
            return 1;
        }
    }
}
