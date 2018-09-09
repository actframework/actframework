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

import static act.db.CriteriaOperator.BuiltIn.*;

import act.db.*;
import org.osgl.$;
import org.osgl.util.*;

import java.util.ArrayList;
import java.util.List;

public class CriteriaUtil {

    public static class _CriteriaNodeStage {
        private String fieldName;
        private _CriteriaNodeStage(String fieldName) {
            this.fieldName = S.requireNotEmpty(fieldName.trim());
        }

        public CriteriaNode between(Object a, Object b) {
            return CriteriaUtil.between(fieldName, a, b);
        }

        public CriteriaNode eq(Object val) {
            return CriteriaUtil.eq(fieldName, val);
        }

        public CriteriaNode equal(Object val) {
            return eq(val);
        }

        public CriteriaNode ne(Object val) {
            return CriteriaUtil.ne(fieldName, val);
        }

        public CriteriaNode neq(Object val) {
            return ne(val);
        }

        public CriteriaNode notEqual(Object val) {
            return ne(val);
        }

        public CriteriaNode gt(Object val) {
            return CriteriaUtil.gt(fieldName, val);
        }

        public CriteriaNode gte(Object val) {
            return CriteriaUtil.gte(fieldName, val);
        }

        public CriteriaNode lt(Object val) {
            return CriteriaUtil.lt(fieldName, val);
        }

        public CriteriaNode lte(Object val) {
            return CriteriaUtil.lte(fieldName, val);
        }

        public CriteriaNode like(Object val) {
            return CriteriaUtil.like(fieldName, val);
        }

        public CriteriaNode unlike(Object val) {
            return CriteriaUtil.unlike(fieldName, val);
        }

        public CriteriaNode in(Object val) {
            return CriteriaUtil.in(fieldName, val);
        }

        public CriteriaNode nin(Object val) {
            return CriteriaUtil.nin(fieldName, val);
        }

        public CriteriaNode notIn(Object val) {
            return nin(val);
        }
    }

    public static _CriteriaNodeStage c(String fieldName) {
        return new _CriteriaNodeStage(fieldName);
    }

    public static CriteriaGroup and(String expression, Object ... params) {
        return new CriteriaGroup(CriteriaGroupLogic.AND, parse(expression, params));
    }

    public static CriteriaGroup and(CriteriaComponent comp1, CriteriaComponent... otherComponents) {
        return new CriteriaGroup(CriteriaGroupLogic.AND, concat(comp1, otherComponents));
    }

    public static CriteriaGroup or(String expression, Object... params) {
        return new CriteriaGroup(CriteriaGroupLogic.OR, parse(expression, params));
    }

    public static CriteriaGroup or(CriteriaComponent comp1, CriteriaComponent... otherComponents) {
        return new CriteriaGroup(CriteriaGroupLogic.OR, concat(comp1, otherComponents));
    }

    public static CriteriaNode between(String fieldName, Object a, Object b) {
        return new CriteriaNode(fieldName, BETWEEN, a, b);
    }

    public static CriteriaNode eq(String fieldName, Object val) {
        return new CriteriaNode(fieldName, EQ, val);
    }

    public static CriteriaNode equal(String fieldName, Object val) {
        return eq(fieldName, val);
    }

    public static CriteriaNode exists(String fieldName) {
        return new CriteriaNode(fieldName, EXISTS);
    }

    public static CriteriaNode gt(String fieldName, Object val) {
        return new CriteriaNode(fieldName, GT, val);
    }

    public static CriteriaNode greaterThan(String fieldName, Object val) {
        return gt(fieldName, val);
    }

    public static CriteriaNode gte(String fieldName, Object val) {
        return new CriteriaNode(fieldName, GTE, val);
    }

    public static CriteriaNode greaterThanOrEqualTo(String fieldName, Object val) {
        return gte(fieldName, val);
    }

    public static CriteriaNode in(String fieldName, List range) {
        return new CriteriaNode(fieldName, IN, range);
    }

    public static CriteriaNode in(String fieldName, Object... range) {
        return new CriteriaNode(fieldName, IN, range);
    }

    public static CriteriaNode like(String fieldName, Object val) {
        return new CriteriaNode(fieldName, LIKE, val);
    }

    public static CriteriaNode lt(String fieldName, Object val) {
        return new CriteriaNode(fieldName, LT, val);
    }

    public static CriteriaNode lessThan(String fieldName, Object val) {
        return lt(fieldName, val);
    }

    public static CriteriaNode lte(String fieldName, Object val) {
        return new CriteriaNode(fieldName, LTE, val);
    }

    public static CriteriaNode lessThanOrEqualTo(String fieldName, Object val) {
        return lte(fieldName, val);
    }

    public static CriteriaNode ne(String fieldName, Object val) {
        return new CriteriaNode(fieldName, NE, val);
    }

    public static CriteriaNode notEqual(String fieldName, Object val) {
        return ne(fieldName, val);
    }

    public static CriteriaNode nin(String fieldName, List range) {
        return new CriteriaNode(fieldName, NIN, range);
    }

    public static CriteriaNode nin(String fieldName, Object... range) {
        return new CriteriaNode(fieldName, NIN, range);
    }

    public static CriteriaNode notIn(String fieldName, List range) {
        return nin(fieldName, range);
    }

    public static CriteriaNode notIn(String fieldName, Object... range) {
        return nin(fieldName, range);
    }

    public static CriteriaNode unlike(String fieldName, Object val) {
        return new CriteriaNode(fieldName, UNLIKE, val);
    }

    /**
     * Parse criteria expression.
     *
     * The expression contains a list of criteria node expression separated by `,`, e.g
     *
     * ```
     * firstName like, age between, score >
     * ```
     *
     * As shown above, the criteria node expression contains two strings:
     * 1. the field name, e.g. `firstName`, `age`, `score`
     * 2. the {@link CriteriaOperator}, e.g. `like`, `between`, `>`.
     *
     * The number of `params` must match the sum of {@link CriteriaOperator#cardinality()}
     * of criteria operators defined in the expression otherwise `IllegalArgumentException`
     * will be triggered.
     *
     * @param expression
     * @param params
     * @return
     */
    private static List<CriteriaComponent> parse(String expression, Object ... params) {
        S.List nodes = S.fastSplit(expression, ",");
        List<CriteriaComponent> components = new ArrayList<>();
        int n = 0;
        int paramCnt = params.length;
        for (String nodeExpression : nodes) {
            CriteriaNode node = CriteriaNode.parse(nodeExpression);
            int cardinality = node.operator().cardinality();
            if (cardinality > 0) {
                E.illegalArgumentIf(paramCnt < n + cardinality, "criteria param number[%s] does not match expression[%s] requirement", paramCnt, expression);
                node.setParams($.subarray(params, n, n + cardinality));
                n += cardinality;
            }
            components.add(node);
        }
        return components;
    }

    private static List<CriteriaComponent> concat(CriteriaComponent first, CriteriaComponent ... others) {
        C.List<CriteriaComponent> retList = C.newList(first);
        if (0 == others.length) {
            return retList;
        }
        return retList.append(C.listOf(others));
    }

}
