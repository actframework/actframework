package act.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate a command argument is optional
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Optional {

    /**
     * Specify the argument lead, e.g.
     * <pre>
     *     {@literal @}Command("account.show")
     *     public List&lt;Account&gt; listAccount({@literal @}Optional({"-q", "--query"}) String q,
     *                                      {@literal @}Optional(value = "-l, --limit", defVal = "-1") int limit) {
     *          Query query = new Query();
     *          if (null != q) {
     *              query.filter("text", q);
     *          }
     *          if (limit > -1) {
     *              query.limit(limit);
     *          }
     *          return Account.find(query).asList();
     *      }
     * </pre>
     * <p>If not specified, then the system will assume the argument lead to be follows</p>
     * <ul>
     *     <li>{@code -o}, where "o" is the first char of the argument name</li>
     *     <li>{@code --option}, where "option" is the full argument name</li>
     * </ul>
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
}
