package act.inject.param;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.app.ActionContext;
import act.util.ActContext;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import java.util.*;

class ParamTree {

    private static final Logger LOGGER = LogManager.get(ParamTree.class);

    private Map<ParamKey, ParamTreeNode> allNodes = new HashMap<>();

    private ParamTreeNode rootNode;

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

    ParamTreeNode node(ParamKey key, ActContext<?> context) {
        ParamTreeNode node = allNodes.get(key);
        if (null == node && context.isAllowIgnoreParamNamespace()) {
            key = key.withoutNamespace();
            if (null == key) {
                // take out URL variable values.
                if (context instanceof ActionContext) {
                    ActionContext actionContext = (ActionContext) context;
                    Map<ParamKey, ParamTreeNode> nonUrlPathVars = new HashMap<>();
                    for (Map.Entry<ParamKey, ParamTreeNode> entry : allNodes.entrySet()) {
                        if (!actionContext.isPathVar(entry.getKey().name())) {
                            nonUrlPathVars.put(entry.getKey(), entry.getValue());
                        }
                    }
                    Map<ParamKey, ParamTreeNode> copy = allNodes;
                    allNodes = nonUrlPathVars;
                    node = asRootNode();
                    allNodes = copy;
                    return node;
                }
                if (null == rootNode) {
                    rootNode = asRootNode();
                }
                node = rootNode;
            } else {
                node = allNodes.get(key);
            }
        }
        return node;
    }

    private ParamTreeNode asRootNode() {
        ParamTreeNode root = ParamTreeNode.map(ParamKey.ROOT_KEY);
        for (Map.Entry<ParamKey, ParamTreeNode> entry : allNodes.entrySet()) {
            ParamKey key = entry.getKey();
            if (key.size() > 1) {
                continue;
            }
            root.addChild(key.name(), entry.getValue());
        }
        return root;
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
        List<String> list = new ArrayList<>();
        int len = rawKey.length();
        boolean inSquare = false;
        S.Buffer token = S.buffer();
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
        if (!token.isEmpty()) {
            addTokenToList(list, token);
        }
        return list.toArray(new String[list.size()]);
    }

    private static void addTokenToList(List<String> list, S.Buffer token) {
        String s = token.toString();
        if (S.notEmpty(s)) {
            list.add(s);
        } else {
            LOGGER.warn("empty index encountered");
        }
        token.delete(0, s.length() + 1);
    }

}
