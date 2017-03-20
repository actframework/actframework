package act.cli.tree;

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

import act.cli.view.CliView;

import java.util.List;

/**
 * Defines an abstract tree node that can be printed
 * using {@link CliView#TREE Tree view}
 */
public interface TreeNode {

    /**
     * Returns {@code id} of this node.
     * <p>
     *     The {@code id} of a node can be used to identify a
     *     node in a certain context In other words children
     *     nodes of the same parent node cannot share a same id
     * </p>
     * @return the node id
     */
    String id();

    /**
     * Returns the label on the current tree node
     * @return the label string
     */
    String label();

    /**
     * Returns a list of child tree node
     * @return the children list
     */
    List<TreeNode> children();
}
