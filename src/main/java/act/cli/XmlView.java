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

import act.util.PropertySpec;

import java.lang.annotation.*;

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
@Inherited
public @interface XmlView {
}
