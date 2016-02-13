package act.cli;

import act.util.PropertySpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a command method return value shall be displayed as a XML object. e.g
 * <pre>
 * {@code
 * <item>
 *      <id>__act_app_stop</id>
 *      <onetime>true</onetime>
 *      <trigger>null</trigger>
 *    </item>
 *    <item>
 *      <id>__act_app_app_act_plugin_loaded</id>
 *      <onetime>true</onetime>
 *      <trigger>null</trigger>
 *    </item>}
 * </pre>
 * <p>
 *     {@code XmlView} can be used in conjunction with {@link PropertySpec}
 *     to export only specified fields
 * </p>
 * @see TableView
 * @see JsonView
 * @see CsvView
 * @see PropertySpec
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface XmlView {
}
