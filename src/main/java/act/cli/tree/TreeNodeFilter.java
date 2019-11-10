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
import org.osgl.util.S;

import java.util.List;

/**
 * A {@code TreeNodeFilter} can be applied to a {@link TreeNode} and
 * check if it applied to the filter
 */
public abstract class TreeNodeFilter extends $.F2<List<? extends TreeNode>, TreeNode, Boolean> {

    /**
     * Apply the filter on a {@code TreeNode} and check the result.
     * <p>This method will call the {@link #test(List, TreeNode)} to get the result</p>
     * @param path a list of tree node that are ancestors of the node
     * @param theNode the tree node to be evaluated
     * @return {@code true} if the node applied to the filter or {@code false} otherwise
     */
    @Override
    public final Boolean apply(List<? extends TreeNode> path, TreeNode theNode) {
        return test(path, theNode);
    }

    /**
     * Sub class should implement the filter logic in this method
     * @param path a list of tree node that are ancestors of the node
     * @param theNode the tree node to be evaluated
     * @return {@code true} if the node applied to the filter or {@code false} otherwise
     */
    protected abstract boolean test(List<? extends TreeNode> path, TreeNode theNode);

    public enum Common {
        ;
        private static String path(List<? extends TreeNode> context, TreeNode theNode) {
            S.Buffer sb = S.buffer();
            for (TreeNode n : context) {
                sb.append(n.id()).append("/");
            }
            sb.append(theNode.label());
            return sb.toString();
        }

        public static TreeNodeFilter pathMatches(final String pattern) {
            return new TreeNodeFilter() {
                @Override
                protected boolean test(List<? extends TreeNode> path, TreeNode theNode) {
                    String s = path(path, theNode);
                    return s.toLowerCase().contains(pattern.toLowerCase()) || s.matches(pattern);
                }
            };
        }

        public static TreeNodeFilter pathMatches(final $.Function<String, Boolean> predicate) {
            return new TreeNodeFilter() {
                @Override
                protected boolean test(List<? extends TreeNode> path, TreeNode theNode) {
                    return predicate.apply(path(path, theNode));
                }
            };
        }

        public static TreeNodeFilter labelMatches(final String pattern) {
            return new TreeNodeFilter() {
                @Override
                protected boolean test(List<? extends TreeNode> path, TreeNode theNode) {
                    String s = theNode.label();
                    return s.toLowerCase().contains(pattern.toLowerCase()) || s.matches(pattern);
                }
            };
        }

        public static TreeNodeFilter labelMatches(final $.Function<String, Boolean> predicate) {
            return new TreeNodeFilter() {
                @Override
                protected boolean test(List<? extends TreeNode> path, TreeNode theNode) {
                    return predicate.apply(theNode.label());
                }
            };
        }
    }
}
