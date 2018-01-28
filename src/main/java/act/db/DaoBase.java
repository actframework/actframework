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

import act.app.security.SecurityContext;
import act.inject.param.NoBind;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.Generics;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

// We can't do this atm, otherwise app developer cannot use EbeanDao, or MorphiaDao directly
//@InheritedStateless
@NoBind
public abstract class DaoBase<ID_TYPE, MODEL_TYPE, QUERY_TYPE extends Dao.Query<MODEL_TYPE, QUERY_TYPE>>
        implements Dao<ID_TYPE, MODEL_TYPE, QUERY_TYPE> {

    private ActContext appCtx;
    private SecurityContext secCtx;
    private boolean destroyed;
    protected Type modelType;
    protected Class<MODEL_TYPE> modelClass;
    protected Type idType;
    protected Class<ID_TYPE> idClass;
    protected Type queryType;
    protected Class<QUERY_TYPE> queryClass;

    public DaoBase() {
        exploreTypes();
    }

    public DaoBase(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType) {
        this.idType = $.notNull(idType);
        this.idClass = Generics.classOf(idType);
        this.modelType = $.notNull(modelType);
        this.modelClass = Generics.classOf(modelType);
    }


    @Override
    public void setAppContext(ActContext context) {
        appCtx = context;
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
        appCtx = null;
        secCtx = null;
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

    @Override
    public void setSecurityContext(SecurityContext context) {
        secCtx = context;
    }

    protected void releaseResources() {}

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    protected final ActContext appContext() {
        return appCtx;
    }

    protected final SecurityContext securityContext() {
        return secCtx;
    }

    @Override
    public Iterable<MODEL_TYPE> findBy(String fields, Object... values) throws IllegalArgumentException {
        return q(fields, values).fetch();
    }

    @Override
    public MODEL_TYPE findOneBy(String fields, Object... values) throws IllegalArgumentException {
        return q(fields, values).first();
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
