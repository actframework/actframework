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

import act.cli.tree.TreeNode;
import act.cli.view.CliView;

import java.util.*;

class ParamTreeNode implements TreeNode {

    private ParamKey key;

    private String leafVal;
    private Map<String, ParamTreeNode> map;
    private List<ParamTreeNode> list;

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
        List<TreeNode> list = new ArrayList<TreeNode>();
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

    String debug() {
        return CliView.TREE.render(this, null, null);
    }

    ParamKey key() {
        return key;
    }

    boolean isLeaf() {
        return null != leafVal;
    }

    String value() {
        return leafVal;
    }

    boolean isMap() {
        return null != map;
    }

    Set<String> mapKeys() {
        return map.keySet();
    }

    Collection<ParamTreeNode> mapValues() {
        return map.values();
    }

    ParamTreeNode child(String name) {
        return map.get(name);
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
