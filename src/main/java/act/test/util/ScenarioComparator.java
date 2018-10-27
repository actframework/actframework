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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScenarioComparator implements Comparator<Scenario> {

    private ScenarioManager scenarioManager;
    private boolean finished;

    public ScenarioComparator(ScenarioManager manager, boolean finished) {
        scenarioManager = $.requireNotNull(manager);
        this.finished = finished;
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
        }
        List<Scenario> d1 = depends(o1, new ArrayList<Scenario>());
        List<Scenario> d2 = depends(o2, new ArrayList<Scenario>());
        if (d1.contains(o2)) {
            return 1;
        }
        if (d2.contains(o1)) {
            return -1;
        }
        int n = o1.partition.compareTo(o2.partition);
        return 0 != n ? n : o1.name.compareTo(o2.name);
    }
    private List<Scenario> depends(Scenario s, List<Scenario> depends) {
        for (String name : s.depends) {
            Scenario scenario = scenarioManager.get(name);
            depends(scenario, depends);
            depends.add(scenario);
        }
        return depends;
    }
}
