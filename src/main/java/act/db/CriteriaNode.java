package act.db;

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

import act.Act;
import act.db.util.CriteriaOperatorRegistry;
import act.db.util.CriteriaUtil;
import org.osgl.$;
import org.osgl.util.*;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

public class CriteriaNode implements CriteriaComponent {
    private String fieldName;
    private CriteriaOperator operator;
    private Object[] params;

    public CriteriaNode(String fieldName, CriteriaOperator operator, Object ... paramValues) {
        this.fieldName = S.requireNotEmpty(fieldName.trim());
        this.operator = $.requireNotNull(operator);
        if (paramValues.length > 0) {
            this.setParams(paramValues);
        } else {
            this.params = new Object[operator.cardinality()];
        }
    }

    @Override
    public CriteriaNode negate() {
        return new CriteriaNode(fieldName, operator.negate(), params);
    }

    @Override
    public int hashCode() {
        return $.hc(fieldName, operator, params);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CriteriaNode) {
            CriteriaNode that = (CriteriaNode) obj;
            return $.eq(that.fieldName, fieldName) && $.eq(that.operator, operator) && $.eq2(that.params, params);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = S.builder();
        print(builder);
        return builder.toString();
    }

    @Override
    public StringBuilder print(StringBuilder buffer) {
        buffer.append(fieldName).append(" ").append(operator);
        if (null != params) {
            switch (params.length) {
                case 0:
                    break;
                case 1:
                    buffer.append(" ").append(params[0]);
                    break;
                case 2:
                    buffer.append(" (").append(params[0]).append(",").append(params[1]).append(")");
                    break;
                default:
                    throw E.unexpected("oops!");
            }
        }
        return buffer;
    }

    @Override
    public void accept(CriteriaVisitor visitor) {
        visitor.visitNode(this);
    }

    public String fieldName() {
        return fieldName;
    }

    public CriteriaOperator operator() {
        return operator;
    }

    public List params() {
        return C.listOf(params);
    }

    public CriteriaNode setParams(List paramValues) {
        return setParams(paramValues.toArray());
    }

    public CriteriaNode setParams(Object... paramValues) {
        int len = paramValues.length;
        E.illegalArgumentIf(len != operator.cardinality());
        if (operator.requireCollection()) {
            Object val = paramValues[0];
            boolean isArray = val.getClass().isArray();
            E.illegalArgumentIfNot((isArray || (val instanceof Collection)), "expect parameter value to be an array or collection");
            if (isArray) {
                E.illegalArgumentIf(Array.getLength(val) == 0, "expected at least one element in array");
            } else {
                E.illegalArgumentIf(((Collection) val).isEmpty(), "expect at least one element in the collection");
            }
        }
        this.params = paramValues;
        return this;
    }

    public CriteriaGroup or(CriteriaNode another) {
        return CriteriaUtil.or(this, another);
    }

    public CriteriaGroup and(CriteriaNode another) {
        return CriteriaUtil.and(this, another);
    }

    public static CriteriaNode parse(String expression) {
        return parse(Act.getInstance(CriteriaOperatorRegistry.class), expression);
    }

    static CriteriaNode parse(CriteriaOperatorRegistry operatorRegistry, String expression) {
        S.Pair pair = split(expression.trim());
        String fieldName = pair.left();
        String operatorStr = pair.right();
        CriteriaOperator operator = null == operatorStr ? CriteriaOperator.BuiltIn.EQ : operatorRegistry.eval(operatorStr);
        return new CriteriaNode(fieldName, operator);
    }

    private static S.Pair split(String expression) {
        int pos = expression.indexOf(" ");
        if (pos < 1) {
            return S.pair(expression, null);
        }
        String left = expression.substring(0, pos);
        String right = expression.substring(pos + 1);
        return S.pair(left, right);
    }
}
