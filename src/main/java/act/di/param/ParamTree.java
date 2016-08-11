package act.di.param;

import act.util.ActContext;
import org.rythmengine.utils.S;

import java.util.*;

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
        ParamKey key = ParamKey.of(parseRawParamKey(rawKey));
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

    /*
     * Parse string like `foo[bar][0][id]` into String array
     * `foo, bar, 0, id`.
     *
     * It shall also support dot notation: `foo.bar.0.id`, or mixed:
     * `foo.bar[0].id`. However things like `foo[0.05]` must be interpreted into
     * `foo, 0.05`  instead of `foo, 0, 05`
     */
    private static String[] parseRawParamKey(String rawKey) {
        List<String> list = new ArrayList<String>();
        int len = rawKey.length();
        boolean inSquare = false;
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < len; ++i) {
            char c = rawKey.charAt(i);
            switch (c) {
                case '.':
                    if (inSquare) {
                        token.append(c);
                    } else {
                        addTokenToList(list, token);
                    }
                    continue;
                case ']':
                    inSquare = false;
                    addTokenToList(list, token);
                    continue;
                case '[':
                    inSquare = true;
                    addTokenToList(list, token);
                    continue;
                default:
                    token.append(c);

            }
        }
        addTokenToList(list, token);
        return list.toArray(new String[list.size()]);
    }

    private static void addTokenToList(List<String> list, StringBuilder token) {
        String s = token.toString();
        if (S.notEmpty(s)) {
            list.add(s);
        }
        token.delete(0, s.length() + 1);
    }

}
