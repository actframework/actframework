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

import java.util.List;

/**
 * A criteria operator is used to define a the relationship between a field and parameter value.
 */
public interface CriteriaOperator {
    /**
     * name of the operator, e.g, "equals"
     */
    String name();

     /**
     * a set of aliases, e.g. `eq` and `==`
     */
    List<String> aliases();

    /**
     * how many parameters is required for this operator
     */
    int cardinality();

    /**
     * Does this operator expect parameter value to be
     * a {@link java.util.Collection} or an array.
     *
     * Only used with `in` operator which has cardinality of `1`.
     */
    boolean requireCollection();

    /**
     * The CriteriaOperator that is an negate of this Criteria Operator.
     */
    CriteriaOperator negate();

    /**
     * Built-in criteria operators
     */
    final class BuiltIn {
        /**
         * `between` - field value shall between two supplied parameters.
         *
         * Aliases:
         *
         * * `>-<`
         */
        public static CriteriaOperator BETWEEN;

        /**
         * `eq` - field value shall be equal to supplied value
         *
         * Aliases:
         *
         * * `=`
         * * `==`
         * * `equal`
         * * `equals`
         */
        public static CriteriaOperator EQ;

        /**
         * `exists` - field value shall be presented. It is up to the
         * provider to interpret the semantic of `exists`. E.g in a SQL database
         * it means `NOT NULL`, in mongodb it means the field must exists but can be `NULL`.
         *
         * aliases:
         *
         * * `exist`
         */
        public static CriteriaOperator EXISTS;

        /**
         * `gt` - field value shall be greater than the supplied value
         *
         * aliases:
         *
         * * `greaterThan`
         * * `great`
         * * `>`
         */
        public static CriteriaOperator GT;

        /**
         * `gte` - field value shall be greater than or equals to the supplied value
         *
         * aliases:
         *
         * * `greaterThanOrEqualTo`
         * * `ge`
         * * `>=`
         * * `=>`
         */
        public static CriteriaOperator GTE;

        /**
         * `in` - field value shall be equal to one of the supplied array/collection
         *
         * aliases: none
         */
        public static CriteriaOperator IN;

        /**
         * `like` - field value shall **like** the supplied value. It is up to
         * the provider to implement the "like" semantic. E.g. it is `LIKE` in
         * SQL system, while in mongodb, it could be regex matching
         *
         * aliases:
         *
         * - `similar`
         * - `~=`
         * - `~~`
         * - `=~`
         */
        public static CriteriaOperator LIKE;

        /**
         * `lt` - field value shall be less than the supplied value.
         *
         * aliases:
         *
         * * `lessThan`
         * * `less`
         * * `<`
         */
        public static CriteriaOperator LT;

        /**
         * `lte` - field value shall be less than or equal to supplied value.
         *
         * aliases:
         *
         * * `lessThanOrEqualTo`
         * * `le`
         * * `lessOrEqual`
         * * `lessOrEquals`
         * * `<=`
         * * `=<`
         */
        public static CriteriaOperator LTE;

        /**
         * `neq` - field value shall not equal to supplied value.
         *
         * aliases:
         *
         * * `ne`
         * * `notEqual`
         * * `notEquals`
         * * `!=`
         * * `<>`
         */
        public static CriteriaOperator NE;

        /**
         * `nin` - field value shall not be any one of the value in the supplied collection/array
         *
         * aliases:
         *
         * * notIn
         */
        public static CriteriaOperator NIN;

        /**
         * `notBetween` - field value shall be outside of the range defined by two supplied values.
         *
         * aliases:
         *
         * * `-X-`
         * * `-x-`
         * * `-><-`
         */
        public static CriteriaOperator NOT_BETWEEN;

        /**
         * `notExists` - field value shall not exists.
         *
         * Note it is up to provider to implement semantic of `not exists`.
         *
         * Aliases:
         *
         * * `notExist`
         *
         * @see #EXISTS
         */
        public static CriteriaOperator NOT_EXISTS;

        /**
         * `unlike` - field value shall not like supplied value.
         *
         * Note it is up to provider to implement semantic of `unlike`
         *
         * Aliases:
         *
         * * `!~`
         * * `notLike`
         *
         * @see #LIKE
         */
        public static CriteriaOperator UNLIKE;
    }
}
