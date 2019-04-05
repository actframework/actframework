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

import org.osgl.$;

public class MetricInfo {

    public static final String HTTP_HANDLER = "act:http";
    public static final String ACT_TEST = "act:test";
    public static final String ACT_TEST_HELPER = "act:test:helper";
    public static final String ACT_TEST_SCENARIO = "act:test:scenario";
    public static final String ACT_TEST_INTERACTION = "act:test:scenario:interaction";
    public static final String CLASS_LOADING = "act:classload";
    public static final String COMPILING = CLASS_LOADING + Metric.PATH_SEPARATOR + "compile";
    public static final String JOB_HANDLER = "act:job";
    public static final String CLI_HANDLER = "act:cli";
    public static final String MAILER = "act:mail";
    public static final String EVENT_HANDLER = "act:event";
    public static final String ROUTING = "act:routing";
    public static final String PATH_SEPARATOR = Metric.PATH_SEPARATOR;

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

    public String getCountAsStr() {
        return CountScale.format(count);
    }

    public long getMsAvg() {
        return getMs() / count;
    }

    public String getAccumulated() {
        return DurationScale.format(ns);
    }

    public String getAvg() {
        return DurationScale.format(ns / count);
    }

    @Override
    public int hashCode() {
        return $.hc(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MetricInfo) {
            MetricInfo that = (MetricInfo) obj;
            return $.eq(name, that.name);
        }
        return false;
    }

    public enum Comparator {
        ;
        public static $.Comparator<MetricInfo> COUNTER = new $.Comparator<MetricInfo>() {
            @Override
            public int compare(MetricInfo m1, MetricInfo m2) {
                long l = m2.count - m1.count;
                if (l < 0) {
                    return -1;
                } else if (l == 0) {
                    return m2.name.compareTo(m1.name);
                }
                return 1;
            }
        };

        public static $.Comparator<MetricInfo> TIMER = new $.Comparator<MetricInfo>() {
            @Override
            public int compare(MetricInfo m1, MetricInfo m2) {
                long l;
                if (null == m1.ns) {
                    l = m2.count - m1.count;
                } else {
                    l = m2.getMsAvg() - m1.getMsAvg();
                }
                if (l < 0) {
                    return -1;
                } else if (l == 0) {
                    return m2.name.compareTo(m1.name);
                } else {
                    return 1;
                }
            }
        };
    }
}
