package act.test.func;

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

import act.Act;
import act.apidoc.ISampleDataCategory;
import act.apidoc.SampleData;
import act.apidoc.SampleDataCategory;
import act.apidoc.SampleDataProvider;
import act.util.SubClassFinder;
import org.osgl.util.C;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public abstract class SampleDataProviderAdaptor extends Func implements Cloneable {

    private SampleDataProvider provider;
    private String name;
    private List<String> aliases;

    private SampleDataProviderAdaptor() {}

    private SampleDataProviderAdaptor(SampleDataProvider provider, ISampleDataCategory category) {
        this.provider = provider;
        this.name = "rand-" + category.name();
        this.aliases = new ArrayList<>();
        this.aliases.add("random-" + category.name());
        Set<String> aliases = category.aliases();
        this.aliases.addAll(C.newList(aliases).map(S.F.prepend("rand-")));
        this.aliases.addAll(C.newList(aliases).map(S.F.prepend("random-")));
    }

    @Override
    protected Keyword keyword() {
        return Keyword.of(name);
    }

    @Override
    public Object apply() {
        return provider.get();
    }

    @Override
    protected List<String> aliases() {
        return aliases;
    }

    @SubClassFinder
    public static void found(SampleDataProvider provider) {
        ISampleDataCategory category = provider.category();
        if (null == category) {
            SampleData.Category anno = provider.getClass().getAnnotation(SampleData.Category.class);
            if (null == anno) {
                return;
            }
            category = anno.value();
        }
        if (category == SampleDataCategory.DOB) {
            Class<?> targetType = provider.targetType();
            // just need to support one Date type
            if (targetType != Date.class) {
                return;
            }
        }
        SampleDataProviderAdaptor adaptor = new SampleDataProviderAdaptor(provider, category){};
        adaptor.register(true);
        Act.app().registerSingleton(adaptor);
    }

}
