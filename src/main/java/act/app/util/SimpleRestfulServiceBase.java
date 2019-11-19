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
import act.app.ActionContext;
import act.data.JodaDateTimeCodec;
import act.data.JodaLocalDateCodec;
import act.data.JodaLocalDateTimeCodec;
import act.data.JodaLocalTimeCodec;
import act.db.*;
import act.inject.param.NoBind;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.mvc.annotation.*;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.E;
import org.osgl.util.Generics;
import org.osgl.util.N;
import org.osgl.util.S;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @NoBind
    private boolean disableDynamicPropSpec;

    public SimpleRestfulServiceBase() {
        exploreTypes();
    }

    public SimpleRestfulServiceBase(DAO_TYPE dao) {
        this.dao = $.requireNotNull(dao);
    }

    protected SimpleRestfulServiceBase(boolean disableDynamicPropSpec) {
        exploreTypes();
        this.disableDynamicPropSpec = disableDynamicPropSpec;
    }

    protected SimpleRestfulServiceBase(DAO_TYPE dao, boolean disableDynamicPropSpec) {
        this.dao = $.requireNotNull(dao);
        this.disableDynamicPropSpec = disableDynamicPropSpec;
    }

    @Before
    public void setPropertySpec(String _propSpec) {
        if (!disableDynamicPropSpec && $.bool(_propSpec)) {
            PropertySpec.current.set(_propSpec);
        }
    }

    /**
     * Sub type can override this method to update the query `q` to ensure
     * the returned data list is only accessible to the current login user.
     *
     * The default implementation return directly.
     *
     * @param q the query object to filter entities been listed.
     */
    protected void onListingEntities(Dao.Query<MODEL_TYPE, ?> q) {}

    /**
     * Sub type can override this method to ensure the entity returned is allowed
     * as per current login user credential.
     *
     * In case permission is not allowed, it shall throw out
     * {@link org.osgl.mvc.result.Forbidden}.
     *
     * The default implementation return directly.
     *
     * @param entity the entity returned.
     */
    protected void onGettingEntity(MODEL_TYPE entity) {}

    /**
     * Sub type can override this method to ensure creating specified
     * model entity is allowed as per current login user credential.
     *
     * In case permission is not allowed, it shall throw out
     * {@link org.osgl.mvc.result.Forbidden}.
     *
     * It could also add logic to further processing the new entity
     * before it is persisted. E.g. associate the entity to the
     * current login user via `email` field etc.
     *
     * The default implementation return directly.
     *
     * @param entity the entity returned.
     */
    protected void onCreatingEntity(MODEL_TYPE entity) {}

    /**
     * Sub type can override this method to ensure updating specified
     * model entity is allowed as per current login user credential.
     *
     * In case permission is not allowed, it shall throw out
     * {@link org.osgl.mvc.result.Forbidden}.
     *
     * It could also inject customized updating logic here, e.g.
     * if the updating is not coming from a full fledged updating
     * data structure, but a simple string, like `description`,
     * then sub type implementation could get the query
     * parameter from `ActionContext.current().req()` instance and
     * finish the updating operation.
     *
     * The default implementation return directly.
     *
     * @param entity the entity returned.
     */
    protected void onUpdatingEntity(MODEL_TYPE entity) {}

    /**
     * Sub type can override this method to ensure deleting specified
     * model entity is allowed as per current login user credential.
     *
     * In case permission is not allowed, it shall throw out
     * {@link org.osgl.mvc.result.Forbidden}.
     *
     * The default implementation return directly.
     *
     * @param entity the entity returned.
     */
    protected void onDeletingEntity(MODEL_TYPE entity) {}

    /**
     * List ${MODEL_TYPE} records, filtered by simple query specifications optionally.
     *
     * ### Filtering
     *
     * To list all model entities simply issue a `GET` request without any query parameters.
     *
     * To filter model by properties, e.g. name: issue `GET path?name=Tom`;
     *
     * To filter model by both name and age, use `GET path?name=Tom&age=5`;
     *
     * Filter with like, less than, great than etc:
     *
     * * `GET path?name=~Tom` - use `~` to lead the string for `LIKE` matching
     * * `GET age=lt_5` - prefix with `lt_` for 'LESS THAN' matching
     * * `GET age=lte_5` - prefix with `lte_` for `LESS THAN OR EQUAL TO` matching
     * * `GET age=gt_5` - prefix with `gt_` for 'GREATER THAN' matching
     * * `GET age=gte_5` - prefix with `gte_` for `GREATER THAN OR EQUAL TO` matching
     *
     * ### Pagination
     *
     * Use `_page` and `_pageSize` to specify the offset and limit of return scope.
     *
     * * `GET path?_page=1&_pageSize=20` - return records starts from 20th, the maximum number returned is 20.
     *
     * ### Sorting
     *
     * Use `_orderBy` to specify sorting orders.
     *
     * * `GET path?_orderBy=-score~name` - The returned list must be sorted by
     *                                     `score` (descending) and then `name`
     *
     * @param _page     the page number - starts from `0`.
     * @param _pageSize the number of items in a page - if not specified then all records filtered returned.
     * @param _orderBy  the sorting list, separated by `~`; if starts with `-` then sort in descending order.
     * @return ${MODEL_TYPE} records as specified above.
     */
    @GetAction
    @PropertySpec("-idAsStr,-v")
    public Iterable<MODEL_TYPE> list(int _page, int _pageSize, String _orderBy) {
        E.illegalArgumentIf(_page < 0, "page number is less than zero");
        E.illegalArgumentIf(_pageSize < 0, "page size is less than zero");
        Dao.Query<MODEL_TYPE, ?> q = filter(dao, ActionContext.current());
        onListingEntities(q);
        if (_pageSize > 0) {
            q.offset(_page * _pageSize).limit(_pageSize);
        }
        if (null != _orderBy) {
            S.List list = S.fastSplit(_orderBy, "~");
            q.orderBy(list.toArray(new String[list.size()]));
        }
        return q.fetch();
    }

    /**
     * Returns a ${MODEL_TYPE} record by id.
     *
     * @param entity a URL path variable specify the id of the record to be returned
     * @return a ${MODEL_TYPE} record specified by URL path variable `model`
     */
    @GetAction("{entity}")
    @PropertySpec("-idAsStr,-v")
    public MODEL_TYPE get(@DbBind MODEL_TYPE entity) {
        onGettingEntity(entity);
        return entity;
    }

    /**
     * Create a ${MODEL_TYPE} record.
     *
     * @param entity the data for the new ${MODEL_TYPE} record.
     * @return the id of the new ${MODEL_TYPE} record.
     */
    @PostAction
    @PropertySpec("id")
    public MODEL_TYPE create(MODEL_TYPE entity) {
        onCreatingEntity(entity);
        return dao.save(entity);
    }

    /**
     * Update a ${MODEL_TYPE} record by id.
     *
     * @param entity the URL path variable specifies the id of the record to be updated.
     * @param data  the update data that will be applied to the ${MODEL_TYPE} record
     */
    @PutAction("{entity}")
    public void update(@DbBind @NotNull MODEL_TYPE entity, MODEL_TYPE data) {
        onUpdatingEntity(entity);
        if (null != data) {
            // we need to check if data is null or not as there are cases that
            // updating is done by specifying a few query parameter, e.g.
            // `description`, `age` etc.
            $.merge(data).filter("-id").to(entity);
        }
        dao.save(entity);
    }

    /**
     * Delete a ${MODEL_TYPE} record by id.
     *
     * @param entity the URL path variable specifies the id of the record to be deleted.
     */
    @DeleteAction("{entity}")
    public void delete(@DbBind @NotNull MODEL_TYPE entity) {
        onDeletingEntity(entity);
        dao.delete(entity);
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

    public static <MODEL_TYPE> Dao.Query<MODEL_TYPE, ?> filter(Dao<?, MODEL_TYPE, ?> dao, ParamValueProvider paramValueProvider) {
        Set<String> filterKeys = paramValueProvider.paramKeys();
        if (filterKeys.isEmpty()) {
            return dao.q();
        }
        JodaDateTimeCodec dateTimeCodec = Act.getInstance(JodaDateTimeCodec.class);
        JodaLocalDateCodec localDateCodec = Act.getInstance(JodaLocalDateCodec.class);
        JodaLocalTimeCodec localTimeCodec = Act.getInstance(JodaLocalTimeCodec.class);
        JodaLocalDateTimeCodec localDateTimeCodec = Act.getInstance(JodaLocalDateTimeCodec.class);
        StringBuilder filters = S.builder();
        List targets = new ArrayList();
        for (String key : filterKeys) {
            if (key.startsWith("_")) {
                continue;
            }
            String[] vals = paramValueProvider.paramVals(key);
            int n = vals.length;
            for (int i = 0; i < n; ++i) {
                String val = vals[i];
                String op = null;
                Object tgt = val;
                if (val.startsWith("~")) {
                    op = "like";
                    tgt = dao.processLikeValue(val.substring(1));
                } else if (val.contains("_")) {
                    String prefix = S.cut(val).beforeFirst("_");
                    N.Comparator comp = N.Comparator.of(prefix);
                    if (null != comp) {
                        op = comp.name();
                        if (comp == N.Comparator.EQ) {
                            op = null;
                        }
                        val = S.cut(val).afterFirst("_");
                    }
                    if (N.isInt(val)) {
                        tgt = Integer.parseInt(val);
                    } else if (N.isNumeric(val)) {
                        tgt = Double.parseDouble(val);
                    } else {
                        // try date time
                        try {
                            tgt = dateTimeCodec.parse(val);
                        } catch (Exception e) {
                            try {
                                tgt = localDateCodec.parse(val);
                            } catch (Exception e1) {
                                try {
                                    tgt = localTimeCodec.parse(val);
                                } catch (Exception e2) {
                                    try {
                                        tgt = localDateTimeCodec.parse(val);
                                    } catch (Exception e3) {
                                        // ignore
                                    }
                                }
                            }
                        }
                    }
                }
                filters.append(key);
                if (null != op) {
                    filters.append(" ").append(op);
                }
                filters.append(",");
                targets.add(tgt);
            }
        }
        if (filters.length() > 0) {
            filters.deleteCharAt(filters.length() - 1); // remove last comma
            return dao.q(filters.toString(), targets.toArray(new Object[targets.size()]));
        }
        return dao.q();
    }

}
