package act.apidoc.sampledata;

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

import act.apidoc.SampleData;
import act.apidoc.SampleDataCategory;
import act.apidoc.SampleDataProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@SampleData.Category(SampleDataCategory.DOB)
public class DobJDKDateProvider extends SampleDataProvider<Date> {

    @Inject
    private DobStringProvider provider;

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public Date get() {
        try {
            return format.parse(provider.get());
        } catch (Exception e) {
            return new Date();
        }
    }
}
