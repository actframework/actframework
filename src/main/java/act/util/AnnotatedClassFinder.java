package act.util;

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

import act.app.event.SysEventId;

import java.lang.annotation.*;

/**
 * The annotation is used on a certain method to mark it
 * as a callback method when a certain class has been found
 * by annotation class specified
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AnnotatedClassFinder {

    /**
     * Specify the "What" to find the class, i.e. the annotation
     * class that has been used to tag the target class
     *
     * @return the annotation class used to find the target classes
     */
    Class<? extends Annotation> value();

    /**
     * Should I collect only public classes?
     *
     * default value is `true`
     *
     * @return `true` if only public class shall be collected, `false` otherwise
     */
    boolean publicOnly() default true;

    /**
     * Should I collect abstract classes?
     *
     * default value is `false`
     *
     * @return `true` if abstract classes shall be excluded, `false` otherwise
     */
    boolean noAbstract() default true;

    /**
     * Specify when to execute the call back for a certain found class.
     * <p>
     * By default the value of `callOn` is {@link SysEventId#DEPENDENCY_INJECTOR_PROVISIONED}
     *
     * @return the "When" to execute the callback logic
     */
    SysEventId callOn() default SysEventId.DEPENDENCY_INJECTOR_PROVISIONED;

}
