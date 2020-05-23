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

import act.Act;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class method as a console command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    /**
     * alias of {@link #name()}
     * @return the command string
     */
    String value() default "";

    /**
     * Returns the name of the command
     * @return the command string
     */
    String name() default "";

    /**
     * @return the help message for the command
     */
    String help() default "";

    /**
     * Specify the ActFramework working mode this command is bind to.
     * <p>
     *     Note {@code DEV} mode command will not available at {@code PROD} mode.
     *     However {@code PROD} mode command is available at {@code DEV} mode
     * </p>
     * @return the actframework working mode
     */
    Act.Mode mode() default Act.Mode.PROD;

    class Util {

    }
}
