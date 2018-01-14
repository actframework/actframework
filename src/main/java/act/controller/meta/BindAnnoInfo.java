package act.controller.meta;

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

import act.app.App;
import org.osgl.mvc.util.Binder;
import org.osgl.util.E;

import java.util.ArrayList;
import java.util.List;

public class BindAnnoInfo extends ParamAnnoInfoTraitBase {
    private List<Class<? extends Binder>> binders = new ArrayList<>();
    private String model;

    public BindAnnoInfo(int index) {
        super(index);
    }

    @Override
    public void attachTo(HandlerParamMetaInfo param) {
        param.bindAnno(this);
    }

    public BindAnnoInfo binder(Class<? extends Binder> binder) {
        E.NPE(binder);
        this.binders.add(binder);
        return this;
    }

    public List<Binder> binder(App app) {
        List<Binder> list = new ArrayList<>();
        for (Class<? extends Binder> binderClass: binders) {
            list.add(app.getInstance(binderClass));
        }
        return list;
    }

    public BindAnnoInfo model(String model) {
        this.model = model;
        return this;
    }

    public String model() {
        return model;
    }
}
