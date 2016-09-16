package act.cli.tree;

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
