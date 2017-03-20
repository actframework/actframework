package act.cli;

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate a command argument is optional
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Optional {

    /**
     * Specify the argument lead, e.g.
     *
     * ```
     *     {@literal @}Command("account.show")
     *     public List<Account> listAccount(
     *         {@literal @}Optional({"-q", "--query"}) String q,
     *         {@literal @}Optional(value = "-l, --limit", defVal = "-1") int limit
     *     ) {
     *          Query query = new Query();
     *          if (null != q) {
     *              query.filter("text", q);
     *          }
     *          if (limit > -1) {
     *              query.limit(limit);
     *          }
     *          return Account.find(query).asList();
     *      }
     * ```
     *
     * If not specified, then the system will assume the argument lead to be follows
     *
     * * {@code -o}, where "o" is the first char of the argument name
     * * {@code --option}, where "option" is the full argument name
     *
     * @return the argument lead as described
     */
    String[] lead() default "";

    /**
     * Specify the default value in String. The framework will automatically convert the String
     * to required type (all primitive types and their correlated classes, String, and Enum types).
     * @return the default value
     * @see #value()
     */
    String defVal() default "";

    /**
     * Alias of {@link #help()}
     * @return the help message
     */
    String value() default "";

    /**
     * Specify the help message for this option
     * @return the help message
     */
    String help() default "";

    /**
     * Specify the error message template when it failed to resolve option
     * value to the required argument.
     *
     * The template must have one `%s` template variable to put in the invalid
     * value
     *
     * @return the error message template
     */
    String errorTemplate() default "";

}
