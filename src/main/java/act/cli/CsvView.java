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
 * **Note** this is deprecated, please use `act.util.CsvView` instead
 *
 * Mark a command method return value shall be displayed using a csv file format. e.g
 *
 * ```
 * id,onetime,trigger
 * __act_app_stop,true,null
 * __act_app_app_act_plugin_loaded,true,null
 * ```
 *
 *
 * {@code CsvView} can be used in conjunction with {@link PropertySpec}
 * to export only specified fields
 *
 *
 * Note if a method is marked with neither {@link CsvView} nor
 * {@link JsonView} then the console will simply use
 * {@link Object#toString()} to present the data.
 *
 * @see JsonView
 * @see TableView
 * @see PropertySpec
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated
@Inherited
public @interface CsvView {
}
