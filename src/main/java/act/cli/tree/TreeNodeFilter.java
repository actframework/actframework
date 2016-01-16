package act.cli.tree;

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
            StringBuilder sb = S.builder();
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
                    return path(path, theNode).matches(pattern);
                }
            };
        }

        public static TreeNodeFilter labelMatches(final String pattern) {
            return new TreeNodeFilter() {
                @Override
                protected boolean test(List<? extends TreeNode> path, TreeNode theNode) {
                    return theNode.label().matches(pattern);
                }
            };
        }
    }
}
