package act.apidoc;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import java.util.Set;

/**
 * Identify a sample data category
 */
public interface ISampleDataCategory {
    /**
     * Returns the name of the sample data category.
     *
     * Note the name of the sample data category must be unique.
     *
     * @return the name of the category.
     */
    String name();

    /**
     * Returns a set of category aliases.
     *
     * Note the alias of a sample data category must not be the alias of another sample data category
     *
     * @return the aliases of the category.
     */
    Set<String> aliases();
}
