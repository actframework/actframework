package act.cli;

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
     * <pre>
     *     {@literal @}Command("account.show")
     *     public Account showAccount({@literal @}Required(value = {"-i", "--id"}, group = "id") String id,
     *                                {@literal @}Required(value = "-e, --email", group = "id") String email) {
     *          if (null != id) {
     *              return Account.findById(id);
     *          } else {
     *              return Account.findByEmail(id);
     *          }
     *      }
     * </pre>
     * <p>In the above command, either {@code id} or {@code email} must be provided, otherwise
     * the framework will throw out {@code IllegalArgumentException}
     * before calling into the method</p>
     * <p>If not specified, then the marked argument must be provided in regarding to other
     * arguments</p>
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
}
