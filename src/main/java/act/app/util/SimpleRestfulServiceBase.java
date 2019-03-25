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
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.*;
import org.osgl.util.Generics;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

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

    /**
     * List all ${MODEL_TYPE} records.
     *
     * @return all ${MODEL_TYPE} records
     */
    @GetAction
    public Iterable<MODEL_TYPE> list() {
        return dao.findAll();
    }

    /**
     * Returns a ${MODEL_TYPE} record by id.
     *
     * @param model
     *      a URL path variable specify the id of the record to be returned
     * @return a ${MODEL_TYPE} record specified by URL path variable `model`
     */
    @GetAction("{model}")
    public MODEL_TYPE get(@DbBind MODEL_TYPE model) {
        return model;
    }

    /**
     * Create a ${MODEL_TYPE} record.
     *
     * @param model
     *      the data for the new ${MODEL_TYPE} record.
     * @return
     *      the id of the new ${MODEL_TYPE} record.
     */
    @PostAction
    @PropertySpec("id")
    public MODEL_TYPE create(MODEL_TYPE model) {
        return dao.save(model);
    }

    /**
     * Update a ${MODEL_TYPE} record by id.
     *
     * @param model
     *      the URL path variable specifies the id of the record to be updated.
     * @param data
     *      the update data that will be applied to the ${MODEL_TYPE} record
     */
    @PutAction("{model}")
    public void update(@DbBind MODEL_TYPE model, MODEL_TYPE data) {
        $.merge(data).filter("-id").to(model);
        dao.save(model);
    }

    /**
     * Delete a ${MODEL_TYPE} record by id.
     * @param id
     *      the URL path variable specifies the id of the record to be deleted.
     */
    @DeleteAction("{id}")
    public void delete(ID_TYPE id) {
        dao.deleteById(id);
    }

    private void exploreTypes() {
        Map<String, Class> typeParamLookup = Generics.buildTypeParamImplLookup(getClass());
        List<Type> types = Generics.typeParamImplementations(getClass(), SimpleRestfulServiceBase.class);
        int sz = types.size();
        if (sz < 3) {
            throw new IllegalArgumentException("Cannot determine DAO type");
        }
        Type daoType = types.get(2);
        BeanSpec spec = BeanSpec.of(daoType, Act.injector(), typeParamLookup);
        DaoLoader loader = Act.getInstance(DaoLoader.class);
        dao = $.cast(loader.load(spec));
    }

}
