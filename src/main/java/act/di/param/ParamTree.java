package act.di.param;

import act.util.ActContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ParamTree {

    private Map<String[], ParamTreeNode> allNodes = new HashMap<>();

    void build(ActContext context) {
        Set<String> paramKeys = context.paramKeys();
        for (String key : paramKeys) {
            String[] vals = context.paramVals(key);
            buildNode(key, vals);
        }
    }

    private void buildNode(String key, String[] vals) {
        String[] path = key.split("[\\[\\].]+");
        ParamTreeNode node;
        int len = vals.length;
        if (len == 0) {
            return;
        }
        if (len > 1) {
            node = ParamTreeNode.list(path);
            for (int i = 0; i < vals.length; ++i) {
                ParamTreeNode leafNode = ParamTreeNode.leaf(path, vals[i]);
                node.addListItem(leafNode);
            }
        } else {
            node = ParamTreeNode.leaf(path, vals[0]);
        }
        allNodes.put(path, node);
        len = path.length;
        if (len == 1) {
            return;
        }
        ParamTreeNode parent = ensureParent(parentPath(path));
        parent.addChild(path[len - 1], node);
    }

    private ParamTreeNode ensureParent(String[] path) {
        ParamTreeNode node = allNodes.get(path);
        if (null == node) {
            node = ParamTreeNode.map(path);
            allNodes.put(path, node);
        }
        int len = path.length;
        if (len > 1) {
            ParamTreeNode parent = ensureParent(parentPath(path));
            parent.addChild(path[len - 1], node);
        }

        return node;
    }

    private static String[] parentPath(String[] path) {
        int len = path.length - 1;
        String[] parentPath = new String[len];
        System.arraycopy(path, 0, parentPath, 0, len);
        return parentPath;
    }

}
