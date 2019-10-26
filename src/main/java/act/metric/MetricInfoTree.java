package act.metric;

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

import act.cli.tree.FilteredTreeNode;
import act.cli.tree.TreeNode;
import act.cli.tree.TreeNodeFilter;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricInfoTree {

    private Map<String, MetricInfo> infoMap = new HashMap<>();
    private MetricInfoNode root;
    private Map<String, MetricInfoNode> nodeMap = new HashMap<>();
    private TreeNodeFilter filter = null;


    public MetricInfoTree(List<MetricInfo> infoList, $.Predicate<String> filter) {
        for (MetricInfo info : infoList) {
            infoMap.put(info.getName(), info);
        }
        if (filter != $.F.TRUE) {
            this.filter = TreeNodeFilter.Common.pathMatches(filter);
        }
        buildTree();
    }

    public TreeNode root(NodeDecorator decorator) {
        return null == filter ? decorator.decorate(root) : new FilteredTreeNode(decorator.decorate(root), filter);
    }

    MetricInfoNode getNode(MetricInfo info) {
        MetricInfoNode node = nodeMap.get(info.getName());
        if (null == node) {
            node = new MetricInfoNode(info);
            nodeMap.put(info.getName(), node);
        }
        return node;
    }

    MetricInfo getMetricInfo(String name) {
        return infoMap.get(name);
    }

    void buildTree() {
        for (Map.Entry<String, MetricInfo> entry: infoMap.entrySet()) {
            String path = entry.getKey();
            MetricInfo info = entry.getValue();
            MetricInfoNode node = getNode(info);
            if (node.isRoot) {
                root = node;
            }
        }
    }

    class MetricInfoNode implements TreeNode {

        MetricInfo info;
        C.List<TreeNode> children = C.newList();
        private boolean isRoot;

        MetricInfoNode(MetricInfo info) {
            this.info = $.requireNotNull(info);
            this.isRoot = !addToParent();
        }

        @Override
        public String id() {
            return S.str(info.getName()).afterLast(Metric.PATH_SEPARATOR).toString();
        }

        @Override
        public String label() {
            return info.getName();
        }

        @Override
        public C.List<TreeNode> children() {
            return children;
        }

        public String getParentPath() {
            String path = info.getName();
            if (path.contains(Metric.PATH_SEPARATOR)) {
                return S.beforeLast(path, Metric.PATH_SEPARATOR);
            }
            return "";
        }

        boolean addToParent() {
            String parentPath = getParentPath();
            if (S.blank(parentPath)) {
                return false;
            }
            MetricInfo parent = getMetricInfo(parentPath);
            if (null == parent) {
                return false;
            }
            MetricInfoNode parentNode = getNode(parent);
            parentNode.children.add(this);
            return true;
        }
    }

    public static class NodeDecorator {
        $.Function<MetricInfo, String> labelGetter;

        private static final TreeNode NULL = new TreeNode() {
            @Override
            public String id() {
                return "";
            }

            @Override
            public String label() {
                return "";
            }

            @Override
            public List<TreeNode> children() {
                return C.list();
            }
        };

        NodeDecorator($.Function<MetricInfo, String> labelGetter) {
            this.labelGetter = $.requireNotNull(labelGetter);
        }

        TreeNode decorate(final MetricInfoNode node) {
            if (null == node) {
                return NULL;
            }
            return new TreeNode() {

                @Override
                public String id() {
                    return node.id();
                }

                @Override
                public String label() {
                    return labelGetter.apply(node.info);
                }

                @Override
                public List<TreeNode> children() {
                    return node.children().map(new $.Transformer<TreeNode, TreeNode>() {
                        @Override
                        public TreeNode transform(TreeNode treeNode) {
                            return decorate((MetricInfoNode) treeNode);
                        }
                    });
                }

            };
        }

    }

    public static final NodeDecorator COUNTER = new NodeDecorator(new $.Transformer<MetricInfo, String>() {
        @Override
        public String transform(MetricInfo metricInfo) {
            return S.fmt("%s: %s", metricInfo.getName(), metricInfo.getCountAsStr());
        }
    });

    public static final NodeDecorator TIMER = new NodeDecorator(new $.Transformer<MetricInfo, String>() {
        @Override
        public String transform(MetricInfo metricInfo) {
            return S.fmt("%s: %s / %s = %s", metricInfo.getName(), metricInfo.getAccumulated(), metricInfo.getCountAsStr(), metricInfo.getAvg());
        }
    });

}
