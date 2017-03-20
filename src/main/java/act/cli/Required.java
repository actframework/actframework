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
 * Indicate a command argument is mandatory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Required {

    /**
     * Specify the argument lead, e.g.
     * <pre>
     *     {@literal @}Command("account.show")
     *     public Account showAccount({@literal @}Required({"-i", "--id"}) String id) {
     *          return Account.findById(id);
     *     }
     * </pre>
     * Developer can also use one string separated by "," to specify two leads:
     * <pre>
     *     {@literal @}Command("account.show")
     *     public Account showAccount({@literal @}Required("-i,--id") String id) {
     *          return Account.findById(id);
     *     }
     * </pre>
     * <p>If not specified, then the system will assume the argument lead to be follows</p>
     * <ul>
     * <li>{@code -o}, where "o" is the first char of the argument name</li>
     * <li>{@code --option}, where "option" is the full argument name</li>
     * </ul>
     *
     * @return the argument lead as described
     */
    String[] lead() default "";

    /**
     * Specify the mutual exclusive group. If there are multiple {@code @Required} options found
     * in the command line with the same group then one and only one of them needs to be provided. If
     * more than one argument is provided then the application may choose anyone of the argument. E.g.
     *
     * ```
     *     {@literal @}Command("account.show")
     *     public Account showAccount(
     *         {@literal @}Required(lead = "-i, --id", group = "id") String id,
     *         {@literal @}Required(lead = "-e, --email", group = "id") String email
     *     ) {
     *          if (null != id) {
     *              return Account.findById(id);
     *          } else {
     *              return Account.findByEmail(id);
     *          }
     *      }
     * ```
     *
     * In the above command, either {@code id} or {@code email} must be provided, otherwise
     * the framework will throw out {@code IllegalArgumentException}
     * before calling into the method
     *
     * If not specified, then the marked argument must be provided
     *
     * @return the mutual exclusive group
     */
    String group() default "";


    /**
     * Alias of {@link #help()}
     *
     * @return the help message
     */
    String value() default "";

    /**
     * Specify the help message for this option
     *
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
