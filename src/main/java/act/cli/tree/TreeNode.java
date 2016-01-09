package act.cli.tree;

import act.cli.view.CliView;

import java.util.List;

/**
 * Defines an abstract tree node that can be printed
 * using {@link CliView#TREE Tree view}
 */
public interface TreeNode {
    /**
     * Returns the label on the current tree node
     * @return the label string
     */
    public String label();

    /**
     * Returns a list of child tree node
     * @return the children list
     */
    public List<TreeNode> children();
}
