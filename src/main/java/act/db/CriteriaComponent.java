package act.db;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import java.io.Serializable;

/**
 * A tag interface mark a unit that can be put into any where in a criteria structure
 */
public interface CriteriaComponent extends Serializable {
    /**
     * Return negate of this component
     */
    CriteriaComponent negate();

    /**
     * Print to the StringBuilder and return the builder
     * @param buffer
     *      the StringBuilder into which the component output String representation
     * @return
     *      the buffer passed in after printed.
     */
    StringBuilder print(StringBuilder buffer);

    /**
     * Accept a visitor to walk through this CriteriaComponent
     *
     * @param visitor the visitor
     */
    void accept(CriteriaVisitor visitor);
}
