package act.apidoc;

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
import act.app.event.SysEventId;
import act.job.OnSysEvent;
import act.util.SubClassFinder;

public class SampleDataProviderFinder {

    @SubClassFinder
    public void foundSampleDataProvider(SampleDataProvider provider) {
        Act.app().sampleDataProviderManager().foundSampleDataProvider(provider);
    }

    @OnSysEvent(SysEventId.EVENT_BUS_INITIALIZED)
    public void reset() {
        Act.app().sampleDataProviderManager().reset();
    }

}
