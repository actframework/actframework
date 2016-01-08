package act.cli;

import act.util.PropertySpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a command method return value shall be displayed as a JSON object. e.g
 * <pre>
 * [
 *  {
 *      "ID": "__act_app_stop",
 *      "onetime": true,
 *      "trigger": null
 *  },
 *  {
 *      "id": "__act_app_app_act_plugin_loaded",
 *      "onetime": true,
 *      "trigger": null
 *  }
 * ]
 * </pre>
 * <p>
 *     {@code JsonView} can be used in conjunction with {@link PropertySpec}
 *     to export only specified fields
 * </p>
 * @see TableView
 * @see PropertySpec
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface JsonView {
}
