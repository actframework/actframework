package act.view;

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

import act.app.ActionContext;
import act.mail.MailerContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Template represents a resource that can be merged with {@link ActionContext application context}
 * and output the result
 */
public interface Template {

    /**
     * Notify framework not to cache template for this handler method.
     *
     * If developer can change template path in the logic of handler method,
     * caching template could cause trouble (i.e. it uses the template of first run).
     * In this case developer must annotate the handler method with `@Template.NoCache`
     * annotation
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface NoCache {
    }

    void merge(ActionContext context);

    String render(ActionContext context);

    String render(MailerContext context);

    boolean supportCache();

}
