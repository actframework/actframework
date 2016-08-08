package act.di.param;

import act.util.ActContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ParamTree {

    private Map<ParamKey, ParamTreeNode> allNodes = new HashMap<>();

    void build(ActContext context) {
        Set<String> paramKeys = context.paramKeys();
        for (String key : paramKeys) {
            String[] vals = context.paramVals(key);
            buildNode(key, vals);
        }
    }

    private void buildNode(String rawKey, String[] vals) {
        ParamKey key = ParamKey.of(rawKey.split("[\\[\\].]+"));
        ParamTreeNode node;
        int len = vals.length;
        if (len == 0) {
            return;
        }
        if (len > 1) {
            node = ParamTreeNode.list(key);
            for (int i = 0; i < vals.length; ++i) {
                ParamTreeNode leafNode = ParamTreeNode.leaf(key, vals[i]);
                node.addListItem(leafNode);
            }
        } else {
            node = ParamTreeNode.leaf(key, vals[0]);
        }
        allNodes.put(key, node);
        len = key.size();
        if (len == 1) {
            return;
        }
        ensureParent(key, node);
    }

    ParamTreeNode node(ParamKey key) {
        return allNodes.get(key);
    }

    private void ensureParent(ParamKey childKey, ParamTreeNode child) {
        ParamKey parentKey = childKey.parent();
        if (null == parentKey) {
            return;
        }
        ParamTreeNode parent = allNodes.get(parentKey);
        if (null == parent) {
            parent = ParamTreeNode.map(parentKey);
            allNodes.put(parentKey, parent);
        }
        parent.addChild(childKey.name(), child);
        int len = parentKey.size();
        if (len > 1) {
            ensureParent(parentKey, parent);
        }
    }

}
