package act.db;

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

import act.inject.param.NoBind;
import act.util.LogSupport;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.Generics;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;

// We can't do this atm, otherwise app developer cannot use EbeanDao, or MorphiaDao directly
//@InheritedStateless
public abstract class DaoBase<ID_TYPE, MODEL_TYPE, QUERY_TYPE extends Query<MODEL_TYPE, QUERY_TYPE>> extends LogSupport implements Dao<ID_TYPE, MODEL_TYPE, QUERY_TYPE> {

    @NoBind private boolean destroyed;
    @NoBind protected Type modelType;
    @NoBind protected Class<MODEL_TYPE> modelClass;
    @NoBind protected Type idType;
    @NoBind protected Class<ID_TYPE> idClass;
    @NoBind protected Type queryType;
    @NoBind protected Class<QUERY_TYPE> queryClass;

    public DaoBase() {
        exploreTypes();
    }

    public DaoBase(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType) {
        this.idType = $.requireNotNull(idType);
        this.idClass = idType;
        this.modelType = $.requireNotNull(modelType);
        this.modelClass = Generics.classOf(modelType);
    }


    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
    }

    @Override
    public Class<ID_TYPE> idType() {
        return idClass;
    }

    @Override
    public Class<MODEL_TYPE> modelType() {
        return modelClass;
    }

    @Override
    public Class<QUERY_TYPE> queryType() {
        return queryClass;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    @Override
    public Iterable<MODEL_TYPE> findBy(String expression, Object... values) throws IllegalArgumentException {
        return q(expression, values).fetch();
    }

    @Override
    public MODEL_TYPE findOneBy(String expression, Object... values) throws IllegalArgumentException {
        return q(expression, values).first();
    }

    @Override
    public Iterable<MODEL_TYPE> findAll() {
        return q().fetch();
    }

    @Override
    public List<MODEL_TYPE> findAllAsList() {
        return C.newList(findAll());
    }

    @Override
    public long count() {
        return q().count();
    }

    @Override
    public long countBy(String fields, Object... values) throws IllegalArgumentException {
        return q(fields, values).count();
    }

    private void exploreTypes() {
        List<Type> types = Generics.typeParamImplementations(getClass(), DaoBase.class);
        int sz = types.size();
        if (sz < 1) {
            return;
        }
        if (sz > 2) {
            queryType = types.get(2);
            queryClass = Generics.classOf(queryType);
        }
        if (sz > 1) {
            modelType = types.get(1);
            modelClass = Generics.classOf(modelType);
        }
        idType = types.get(0);
        idClass = Generics.classOf(idType);
    }

}
