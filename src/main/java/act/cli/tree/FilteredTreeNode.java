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

import org.osgl.$;
import org.osgl.util.C;

import java.util.ArrayList;
import java.util.List;

public class FilteredTreeNode implements TreeNode {
    private C.List<TreeNode> path;
    private TreeNodeFilter filter;
    private TreeNode theNode;

    public FilteredTreeNode(TreeNode root, TreeNodeFilter filter) {
        this(C.<TreeNode>list(), root, filter);
    }

    public FilteredTreeNode(List<TreeNode> path, TreeNode theNode, TreeNodeFilter filter) {
        this.theNode = $.requireNotNull(theNode);
        this.filter = $.requireNotNull(filter);
        this.path = C.list($.requireNotNull(path));
    }

    @Override
    public String id() {
        return theNode.id();
    }

    @Override
    public String label() {
        return theNode.label();
    }

    @Override
    public List<TreeNode> children() {
        List<TreeNode> filteredChildren = new ArrayList<>();
        C.List<TreeNode> pathToKid = path.append(theNode);
        for (TreeNode kid : theNode.children()) {
            if (hasAppliedChild(pathToKid, kid)) {
                filteredChildren.add(new FilteredTreeNode(pathToKid, kid, filter));
            }
        }
        return filteredChildren;
    }

    private boolean applied(C.List<TreeNode> path, TreeNode theNode) {
        return filter.apply(path, theNode);
    }

    private boolean hasAppliedChild(C.List<TreeNode> path, TreeNode theNode) {
        if (applied(path, theNode)) {
            return true;
        }
        List<TreeNode> children = theNode.children();
        C.List<TreeNode> pathToKid = path.append(theNode);
        for (TreeNode kid : children) {
            if (hasAppliedChild(pathToKid, kid)) {
                return true;
            }
        }
        return false;
    }
}
