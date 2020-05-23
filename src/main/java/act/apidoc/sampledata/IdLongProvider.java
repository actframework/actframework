package act.apidoc.sampledata;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2020 ActFramework
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

import act.apidoc.SampleData;
import act.apidoc.SampleDataCategory;
import act.apidoc.SampleDataProvider;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@SampleData.Category(SampleDataCategory.ID)
public class IdLongProvider extends SampleDataProvider<Long> {

    private final AtomicLong _id = new AtomicLong();

    @Override
    public Long get() {
        return _id.incrementAndGet();
    }
    
}
