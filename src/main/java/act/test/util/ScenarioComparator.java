package act.test.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.test.Scenario;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScenarioComparator implements Comparator<Scenario> {

    private String finalPartition; // any test scenario fall into this partition shall be run last
    private boolean finished;

    public ScenarioComparator(boolean finished) {
        this.finished = finished;
    }

    public ScenarioComparator(String finalPartition) {
        this.finished = false;
        this.finalPartition = S.requireNotBlank(finalPartition);
    }

    @Override
    public int compare(Scenario o1, Scenario o2) {
        if (finished) {
            boolean p1 = o1.status.pass();
            boolean p2 = o2.status.pass();
            if (!p1 && p2) {
                return -1;
            } else if (p1 && !p2) {
                return 1;
            }
            if ($.bool(o1.ignore) && $.not(o2.ignore)) {
                return -1;
            } else if ($.bool(o2.ignore)) {
                return 1;
            }
        }
        if (o1.allDepends.contains(o2)) {
            return 1;
        }
        if (o2.allDepends.contains(o1)) {
            return -1;
        }
        int n = o1.partition.compareTo(o2.partition);
        if (n != 0) {
            if (null != finalPartition) {
                if (S.eq(o1.partition, finalPartition)) {
                    return 1;
                } else if (S.eq(o2.partition, finalPartition)) {
                    return -1;
                }
            }
            return n;
        }
        return o1.title().compareTo(o2.title());
    }

}
