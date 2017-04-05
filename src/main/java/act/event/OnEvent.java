package act.event;

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
 * Mark a method to be an adhoc event handler for a certain event type
 * The event type is inferred from the method signature
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OnEvent {

    /**
     * <p>Indicate if the handler should be run synchronously with application
     * or asynchronously.</p>
     * <p>Running job synchronously means the application
     * will not start servicing incoming requests until the job is finished.</p>
     * <p>Running job asynchronously means the job will start in a separate thread
     * and will not block the app from servicing incoming requests</p>
     * @return {@code true} if the annotated method shall be executed asynchronously
     *          or {@code false} if the method all be executed synchronously
     */
    boolean async() default false;

    /**
     * When specified as `true` then this is to handle event that triggered before
     * application get started and it shall get hooked immediately after classes has
     * been loaded
     * @return `true` if the event handler needs to be registered early before app started
     */
    boolean beforeAppStart() default false;

}
