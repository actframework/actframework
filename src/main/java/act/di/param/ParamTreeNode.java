package act.di.param;

import act.cli.tree.TreeNode;
import act.cli.view.CliView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParamTreeNode implements TreeNode {

    private ParamKey key;

    private String leafVal;
    private Map<String, ParamTreeNode> map;
    private List<ParamTreeNode>  list;

    private ParamTreeNode(ParamKey path) {
        this.key = path;
    }

    @Override
    public String id() {
        return key.toString();
    }

    @Override
    public String label() {
        return id();
    }

    @Override
    public List<TreeNode> children() {
        List<TreeNode> list = new ArrayList<>();
        if (isList()) {
            list.addAll(this.list);
        } else if (isMap()) {
            list.addAll(map.values());
        }
        return list;
    }

    @Override
    public String toString() {
        return id();
    }

    public String debug() {
        return CliView.TREE.render(this, null, null);
    }

    public void print() {
        System.out.println(debug());
    }

    boolean isLeaf() {
        return null != leafVal;
    }

    boolean isMap() {
        return null != map;
    }

    boolean isList() {
        return null != list;
    }

    List<ParamTreeNode> list() {
        return list;
    }

    void addListItem(ParamTreeNode item) {
        list.add(item);
    }

    void addChild(String key, ParamTreeNode child) {
        map.put(key, child);
    }

    static ParamTreeNode leaf(ParamKey key, String value) {
        ParamTreeNode node = new ParamTreeNode(key);
        node.leafVal = value;
        return node;
    }

    static ParamTreeNode map(ParamKey key) {
        ParamTreeNode node = new ParamTreeNode(key);
        node.map = new HashMap<>();
        return node;
    }

    static ParamTreeNode list(ParamKey key) {
        ParamTreeNode node = new ParamTreeNode(key);
        node.list = new ArrayList<>();
        return node;
    }
}
