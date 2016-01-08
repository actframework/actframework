package act.cli;

import act.util.PropertySpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a command method return value shall be displayed as tree structure. e.g
 * <p>
 *     {@code TreeView} should be used only on object that are type of
 *     {@link act.cli.meta.CommandMethodMetaInfo.View#TREE}
 * </p>
 * @see TableView
 * @see PropertySpec
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface TreeView {
}
