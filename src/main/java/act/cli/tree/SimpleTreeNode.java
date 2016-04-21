package act.cli.tree;

import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

/**
 * A simple mutable tree node implementation
 */
public class SimpleTreeNode implements TreeNode {

    private String id;
    private String label;
    private C.List<TreeNode> children;

    public SimpleTreeNode() {
        this.children = C.newList();
    }

    public SimpleTreeNode(String id, String label, List<TreeNode> children) {
        this.id = id;
        this.label = label;
        this.children = null == children ? C.<TreeNode>newList() : C.newList(children);
    }

    @Override
    public String id() {
        return id;
    }

    public SimpleTreeNode id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String label() {
        return null == label ? id : label;
    }

    public SimpleTreeNode label(String label) {
        this.label = label;
        return this;
    }

    @Override
    public List<TreeNode> children() {
        return C.list(children);
    }

    public SimpleTreeNode children(List<TreeNode> children) {
        this.children = C.newList(children);
        return this;
    }

    public SimpleTreeNode addChild(TreeNode child) {
        this.children.add(child);
        return this;
    }

    public SimpleTreeNode removeChild(TreeNode child) {
        this.children.remove(child);
        return this;
    }

    public SimpleTreeNode removeChild(final String id) {
        this.children.remove(new $.Predicate<TreeNode>() {
            @Override
            public boolean test(TreeNode treeNode) {
                return S.eq(treeNode.id(), id);
            }
        });
        return this;
    }
}
