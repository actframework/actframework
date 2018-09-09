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

public interface CriteriaVisitor {

    /**
     * Mark start of a {@link CriteriaGroup}.
     *
     * @param group
     *      A group
     */
    void beginGroup(CriteriaGroup group);

    /**
     * Mark end of a {@link CriteriaGroup}.
     */
    void endGroup();

    /**
     * Visit {@link CriteriaNode}.
     *
     * @param node
     *      the criteria node
     */
    void visitNode(CriteriaNode node);
}
