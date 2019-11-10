package act.monitor;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.conf.AppConfig;
import act.controller.annotation.Port;
import act.controller.annotation.UrlContext;
import org.osgl.inject.annotation.TypeOf;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.E;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

@Singleton
@UrlContext("probes")
@Port(AppConfig.PORT_SYS)
public class Monitor implements Runnable {

    @TypeOf(Probe.class)
    private List<Probe> probes;

    Map<String, Map<String, Object>> currentStatus = new HashMap<>();

    public Monitor() {
        if (Act.app().config().monitorEnabled()) {
            Act.app().jobManager().every(this, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        for (Probe probe : probes) {
            Map<String, Object> status = probe.doJob();
            currentStatus.put(probe.id(), status);
        }
    }

    @GetAction("{id}")
    public Map<String, Object> get(String id) {
        E.illegalArgumentIf(null == id);
        return currentStatus.get(id);
    }

}
