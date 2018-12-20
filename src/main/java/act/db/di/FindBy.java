package act.db.di;

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
import act.app.App;
import act.cli.CliContext;
import act.db.Dao;
import act.inject.SessionVariable;
import act.inject.param.JsonDto;
import act.inject.param.ParamValueLoaderService;
import act.util.ActContext;
import act.view.ActBadRequest;
import act.view.ActNotFound;
import org.osgl.$;
import org.osgl.inject.ValueLoader;
import org.osgl.util.*;

import java.util.ArrayList;
import java.util.Collection;
import javax.validation.constraints.NotNull;

public class FindBy extends ValueLoader.Base {

    private String requestParamName;
    private String queryFieldName;
    private Dao dao;
    private StringValueResolver resolver;
    private boolean findOne;
    private boolean byId;
    private Class<?> rawType;
    private boolean notNull;
    private String onetimeValue;
    private boolean isSessionVariable;
    private String sessionVarName;

    @Override
    protected void initialized() {
        App app = App.instance();

        rawType = spec.rawType();
        notNull = spec.hasAnnotation(NotNull.class);
        findOne = !(Iterable.class.isAssignableFrom(rawType));
        dao = app.dbServiceManager().dao(findOne ? rawType : (Class) spec.typeParams().get(0));

        queryFieldName = S.string(options.get("field"));
        byId = findOne && S.blank(queryFieldName) && (Boolean) options.get("byId");
        resolver = app.resolverManager().resolver(byId ? dao.idType() : (Class) options.get("fieldType"));
        if (null == resolver) {
            throw new IllegalArgumentException("Cannot find String value resolver for type: " + dao.idType());
        }
        requestParamName = S.string(value());
        if (S.blank(requestParamName)) {
            requestParamName = ParamValueLoaderService.bindName(spec);
        }
        if (!byId) {
            if (S.blank(queryFieldName)) {
                queryFieldName = requestParamName;
            }
        }
        SessionVariable sessionVariable = spec.getAnnotation(SessionVariable.class);
        if (null != sessionVariable) {
            isSessionVariable = true;
            sessionVarName = sessionVariable.value();
            if (S.blank(sessionVarName)) {
                sessionVarName = requestParamName;
            }
        }
    }

    public void setOnetimeValue(String s) {
        onetimeValue = s;
    }

    @Override
    public Object get() {
        ActContext ctx = ActContext.Base.currentContext();
        E.illegalStateIf(null == ctx);
        String value = rawValue(ctx);
        onetimeValue = null;
        if (S.empty(value)) {
            if (findOne) {
                return ensureNotNull(null, value, ctx);
            }
        }
        Object by = null == value ? null : resolver.resolve(value);
        if (findOne) ensureNotNull(by, value, ctx);
        Collection col = null;
        if (!findOne) {
            if (rawType.equals(Iterable.class)) {
                if (S.empty(value)) {
                    return dao.findAll();
                } else {
                    col = new ArrayList();
                }
            } else {
                col = (Collection) App.instance().getInstance(rawType);
            }
        }
        if (null == by) {
            return null;
        }
        if (byId) {
            Object bean = dao.findById(by);
            return ensureNotNull(bean, value, ctx);
        } else {
            if (findOne) {
                Object found = dao.findOneBy(Keyword.of(queryFieldName).javaVariable(), by);
                if (null == found) {
                    // try find by id anyway
                    try {
                        found = dao.findById(by);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                return ensureNotNull(found, value, ctx);
            } else {
                if (S.empty(value)) {
                    col.addAll(dao.findAllAsList());
                } else {
                    col.addAll(C.list(dao.findBy(Keyword.of(queryFieldName).javaVariable(), by)));
                }
                return col;
            }
        }
    }

    private String rawValue(ActContext ctx) {
        if (null != onetimeValue) {
            return onetimeValue;
        }
        if (isSessionVariable) {
            if (ctx instanceof ActionContext) {
                ActionContext actionContext = $.cast(ctx);
                return actionContext.session(sessionVarName);
            } else if (ctx instanceof CliContext) {
                CliContext cliContext = $.cast(ctx);
                return cliContext.session(sessionVarName);
            }
            throw E.unsupport();
        }
        return resolve(requestParamName, ctx);
    }

    private Object ensureNotNull(Object obj, String value, ActContext<?> ctx) {
        if (notNull) {
            if (null == value) {
                String errMsg = Act.appConfig().i18nEnabled() ? ctx._act_i18n("e400.db_bind.missing_request_param", requestParamName) : "missing required parameter: " + requestParamName;
                throw ActBadRequest.create(errMsg);
            }
            if (null == obj) {
                String errMsg = Act.appConfig().i18nEnabled() ? ctx._act_i18n("e404.db_bind.not_found", queryFieldName, value) : "db record not found by " + queryFieldName + " using value: " + value;
                throw ActNotFound.create(errMsg);
            }
        }
        return obj;
    }

    private static String resolve(String bindName, ActContext ctx) {
        String value = ctx.paramVal(bindName);
        if (S.notEmpty(value)) {
            return value;
        }

        if (ctx instanceof ActionContext) {
            ActionContext actionContext = (ActionContext) ctx;
            JsonDto dto = actionContext.attribute(JsonDto.CTX_ATTR_KEY);
            if (null != dto) {
                value = S.string(dto.get(bindName));
            }
        }

        if (S.notEmpty(value)) {
            return value;
        }

        Keyword keyword = Keyword.of(bindName);
        if (keyword.tokens().size() > 1) {
            return resolve(keyword, ctx);
        } else {
            keyword = Keyword.of(bindName + " id");
            return resolve(keyword, ctx);
        }
    }

    private static String resolve(Keyword keyword, ActContext ctx) {
        String value = ctx.paramVal(keyword.underscore());
        if (S.notBlank(value)) {
            return value;
        }
        value = ctx.paramVal(keyword.javaVariable());
        if (S.notBlank(value)) {
            return value;
        }
        return null;
    }

}
