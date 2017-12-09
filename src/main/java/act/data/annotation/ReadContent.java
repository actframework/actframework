package act.data.annotation;

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

import act.data.ContentLinesBinder;
import act.data.ContentLinesResolver;
import act.data.ContentStringBinder;
import act.data.ContentStringResolver;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Resolve;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate if the parameter is a file path or resource URL, then read the content of the file into the parameter,
 * otherwise leave the parameter as it is unless {@link #mercy()} is set to `true`
 *
 * Note this assumes the file or URL resource is a text file
 */
@Resolve({ContentStringResolver.class, ContentLinesResolver.class})
@Bind({ContentStringBinder.class, ContentLinesBinder.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface ReadContent {

    String ATTR_MERCY = "mercy";

    /**
     * If the target does not exists and `mercy` is set to true, the framework
     * will try to inject any query or post parameter that matches the model name
     *
     * default value: `false`
     */
    boolean mercy() default false;
}
