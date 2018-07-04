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
import org.osgl.$;
import org.osgl.util.IntRange;
import org.osgl.util.S;

import javax.inject.Singleton;

@Singleton
@SampleData.Category(SampleDataCategory.DOB)
public class DobStringProvider extends SampleDataProvider<String> {
    @Override
    public String get() {
        return S.join("-", randYear(), randMon(), randDoM());
    }

    private String randYear() {
        return S.string($.random(IntRange.of(1940, 2018)));
    }

    private String randMon() {
        return S.string($.random(IntRange.of(1, 13)));
    }

    private String randDoM() {
        return S.string($.random(IntRange.of(1, 31)));
    }
}
