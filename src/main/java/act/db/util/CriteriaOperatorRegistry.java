package act.db.util;

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

import act.db.CriteriaOperator;
import org.osgl.$;
import org.osgl.util.*;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import javax.inject.Singleton;

/**
 * Keep all registered {@link act.db.CriteriaOperator criteria operators}
 */
@Singleton
public class CriteriaOperatorRegistry {

    public static CriteriaOperatorRegistry INSTANCE;

    static final String CRITERIA_OPERATORS_YAML_FILE = "act/db/util/criteria_operators.yml";

    private Map<Keyword, CriteriaOperator> repo = new HashMap<>();

    public CriteriaOperatorRegistry() {
        INSTANCE = this;
        registerBuiltInOperators();
    }

    public synchronized void register(CriteriaOperator operator) {
        register(operator.name(), operator);
        for (String alias : operator.aliases()) {
            register(alias, operator);
        }
    }

    public CriteriaOperator eval(String name) {
        E.illegalArgumentIf(S.blank(name));
        CriteriaOperator op = repo.get(Keyword.of(name));
        E.illegalArgumentIf(null == op, "Unknown criterial operator: " + name);
        return op;
    }

    private void register(String name, CriteriaOperator operator) {
        Keyword keyword = Keyword.of(name);
        CriteriaOperator existing = repo.put(keyword, operator);
        E.illegalStateIf(null != existing && existing != operator, "operator name already registered: " + name);
    }

    private void registerBuiltInOperators() {
        List<DefaultCriteriaOperatorImpl> builtInOperators = loadBuiltInCriteraOperators();
        Map<Keyword, Field> builtInConstantFields = builtInConstantFields();
        for (CriteriaOperator op : builtInOperators) {
            register(op);
            Field field = findBuiltInField(op, builtInConstantFields);
            if (null != field) {
                $.setStaticFieldValue(field, op);
            }
        }
    }

    private Map<Keyword, Field> builtInConstantFields() {
        Field[] fields = CriteriaOperator.BuiltIn.class.getDeclaredFields();
        Map<Keyword, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            if (CriteriaOperator.class == field.getType()) {
                fieldMap.put(Keyword.of(field.getName()), field);
            }
        }
        return fieldMap;
    }

    private Field findBuiltInField(CriteriaOperator op, Map<Keyword, Field> builtInConstantFields) {
        Field field = builtInConstantFields.get(Keyword.of(op.name()));
        if (null != field) {
            return field;
        }
        for (String alias : op.aliases()) {
            field = builtInConstantFields.get(Keyword.of(alias));
            if (null != field) {
                return field;
            }
        }
        return null;
    }

    private static List<DefaultCriteriaOperatorImpl> loadBuiltInCriteraOperators() {
        URL url = CriteriaOperatorRegistry.class.getClassLoader().getResource(CRITERIA_OPERATORS_YAML_FILE);
        Object o = new Yaml().load(IO.readContentAsString(url));
        List<DefaultCriteriaOperatorImpl> operators = new ArrayList<>();
        $.map(o).targetGenericType(new TypeReference<List<DefaultCriteriaOperatorImpl>>() {
        }).to(operators);
        return operators;
    }
}
