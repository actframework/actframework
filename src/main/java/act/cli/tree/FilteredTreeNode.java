package act.cli.tree;

import org.osgl.$;
import org.osgl.util.C;

import java.util.List;

public class FilteredTreeNode implements TreeNode {
    private C.List<TreeNode> path;
    private TreeNodeFilter filter;
    private TreeNode theNode;

    public FilteredTreeNode(TreeNode root, TreeNodeFilter filter) {
        this(C.<TreeNode>list(), root, filter);
    }

    public FilteredTreeNode(List<TreeNode> path, TreeNode theNode, TreeNodeFilter filter) {
        this.theNode = $.notNull(theNode);
        this.filter = $.notNull(filter);
        this.path = C.list($.notNull(path));
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
        List<TreeNode> filteredChildren = C.newList();
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
