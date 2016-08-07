package act.di.param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParamTreeNode {

    private String[] path;

    private String leafVal;
    private Map<String, ParamTreeNode> map;
    private List<ParamTreeNode>  list;

    private ParamTreeNode(String[] path) {
        this.path = path;
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

    void addListItem(ParamTreeNode item) {
        list.add(item);
    }

    void addChild(String key, ParamTreeNode child) {
        map.put(key, child);
    }

    static ParamTreeNode leaf(String[] path, String value) {
        ParamTreeNode node = new ParamTreeNode(path);
        node.leafVal = value;
        return node;
    }

    static ParamTreeNode map(String[] path) {
        ParamTreeNode node = new ParamTreeNode(path);
        node.map = new HashMap<>();
        return node;
    }

    static ParamTreeNode list(String[] path) {
        ParamTreeNode node = new ParamTreeNode(path);
        node.list = new ArrayList<>();
        return node;
    }
}
