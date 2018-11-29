package act.app.util;

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

import act.Act;
import act.db.*;
import act.inject.param.NoBind;
import act.util.PropertySpec;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.*;
import org.osgl.util.Generics;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A class template for simple RESTful service
 */
public abstract class
SimpleRestfulServiceBase<
        ID_TYPE,
        MODEL_TYPE,
        DAO_TYPE extends DaoBase<ID_TYPE, MODEL_TYPE, ?>> {

    @NoBind
    protected DAO_TYPE dao;

    public SimpleRestfulServiceBase() {
        exploreTypes();
    }

    public SimpleRestfulServiceBase(DAO_TYPE dao) {
        this.dao = $.requireNotNull(dao);
    }

    @GetAction
    public Iterable<MODEL_TYPE> list() {
        return dao.findAll();
    }

    @GetAction("{id}")
    public MODEL_TYPE get(@DbBind("id") MODEL_TYPE model) {
        return model;
    }

    @PostAction
    @PropertySpec("id")
    public MODEL_TYPE create(MODEL_TYPE model) {
        return dao.save(model);
    }

    @PutAction("{id}")
    public void update(@DbBind("id") MODEL_TYPE model, JSONObject data) {
        $.merge(data).to(model);
        dao.save(model);
    }

    @DeleteAction("{id}")
    public void delete(ID_TYPE id) {
        dao.deleteById(id);
    }

    private void exploreTypes() {
        List<Type> types = Generics.typeParamImplementations(getClass(), SimpleRestfulServiceBase.class);
        int sz = types.size();
        if (sz < 3) {
            throw new IllegalArgumentException("Cannot determine DAO type");
        }
        Type daoType = types.get(2);
        BeanSpec spec = BeanSpec.of(daoType, Act.injector());
        DaoLoader loader = Act.getInstance(DaoLoader.class);
        dao = $.cast(loader.load(spec));
    }

}
