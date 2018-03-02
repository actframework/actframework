package act.cli.view;

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

import act.cli.CliContext;
import act.cli.CliOverHttpContext;
import act.cli.ascii_table.impl.CollectionASCIITableAware;
import act.cli.tree.TreeNode;
import act.cli.util.CliCursor;
import act.cli.util.TableCursor;
import act.data.DataPropertyRepository;
import act.db.AdaptiveRecord;
import act.util.ActContext;
import act.util.JsonUtilConfig;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.Output;
import org.osgl.util.S;
import org.rythmengine.utils.Escape;

import java.util.*;

/**
 * Define how the command method return result should
 * be presented
 */
public enum CliView {
    /**
     * present the result using {@link act.cli.TableView}
     */
    TABLE() {
        @Override
        @SuppressWarnings("unchecked")
        public void render(Output output, Object result, PropertySpec.MetaInfo spec, ActContext context) {

            if (null == result) {
                return;
            }

            spec = PropertySpec.MetaInfo.withCurrent(spec, context);

            if (null == spec) {
                spec = new PropertySpec.MetaInfo();
            }

            if (!(context instanceof CliContext)) {
                throw E.unsupport("TableView support in CliContext only. You context: ", context.getClass());
            }

            CliContext cliContext = (CliContext) context;

            List dataList = toList(result);
            int pageSize = context instanceof CliOverHttpContext ? dataList.size() : context.config().cliTablePageSize();
            if (dataList.size() > pageSize) {
                TableCursor cursor = new TableCursor(dataList, pageSize, spec);
                cliContext.session().cursor(cursor);
                cursor.output(cliContext);
                return;
            }
            Class<?> componentType;
            if (dataList.isEmpty()) {
                return;
            }
            componentType = Object.class;
            for (Object o : dataList) {
                if (null != o) {
                    componentType = o.getClass();
                    break;
                }
            }
            DataPropertyRepository repo = context.app().service(DataPropertyRepository.class);
            List<String> outputFields = repo.outputFields(spec, componentType, context);
            if (outputFields.isEmpty()) {
                outputFields = C.list("this as Item");
            }
            String tableString = cliContext.getTable(new CollectionASCIITableAware(dataList, outputFields, spec.labels(outputFields, context)));
            int itemsFound = dataList.size();
            CliCursor cursor = cliContext.session().cursor();
            String appendix = "";
            if (null != cursor) {
                itemsFound = cursor.records();
                appendix = cursor.hasNext() ? "\nType \"it\" for more" : "";
            }
            output.append(S.concat(tableString, "Items found: ", S.string(itemsFound), appendix));
        }

    },

    /**
     * Present data in a Tree structure.
     * <p>
     * Note the {@code result} parameter must be a root {@link act.cli.tree.TreeNode node} of the tree,
     * otherwise the data will be presented in
     * </p>
     * <ul>
     * <li>{@link #TABLE Table view} if the result is an {@link Iterable}, or</li>
     * <li>{@link #JSON JSON view} otherwise</li>
     * </ul>
     */
    TREE() {
        @Override
        public void render(Output output, Object result, PropertySpec.MetaInfo spec, ActContext context) {
            if (result instanceof TreeNode) {
                toTreeString(output, (TreeNode) result);
            } else if (result instanceof Iterable) {
                TABLE.render(output, result, spec, context);
            } else if (null != spec) {
                JSON.render(output, result, spec, context);
            } else {
                output.append(S.string(result));
            }
        }

        private void toTreeString(Output output, TreeNode result) {
            buildTree(output, result, "", true);
        }

        private void buildTree(Output output, TreeNode node, String prefix, boolean isTrail) {
            StringBuilder sb = S.newBuilder().append(prefix).append(isTrail ? "└── " : "├── ").append(node.label()).append("\n");
            output.append(sb);
            List<TreeNode> children = node.children();
            int sz = children.size();
            if (sz == 0) {
                return;
            }
            final String subPrefix = S.newBuilder().append(prefix).append(isTrail ? "    " : "│   ").toString();
            for (int i = 0; i < sz - 1; ++i) {
                TreeNode child = children.get(i);
                buildTree(output, child, subPrefix, false);
            }
            buildTree(output, children.get(sz - 1), subPrefix, true);
        }
    },

    /**
     * Present the result using {@link act.cli.XmlView}
     */
    XML() {
        @Override
        public void render(Output output, Object result, PropertySpec.MetaInfo spec, ActContext context) {
            throw E.unsupport();
        }
    },

    /**
     * Present the result using {@link act.util.JsonView}
     */
    JSON() {
        @Override
        public void render(Output output, Object result, PropertySpec.MetaInfo spec, ActContext context) {
            render(output, result, spec, context, context instanceof CliContext);
        }

        public void render(Output output, Object result, PropertySpec.MetaInfo spec, ActContext context, boolean format) {
            new JsonUtilConfig.JsonWriter(result, spec, format, context).apply(output);
        }
    },

    /**
     * Present the result using {@link Object#toString()}
     */
    TO_STRING() {
        @Override
        public void render(Output output, Object result, PropertySpec.MetaInfo filter, ActContext context) {
            if (result instanceof Iterable) {
                TABLE.render(output, result, filter, context);
            } else if (result instanceof TreeNode) {
                TREE.render(output, result, filter, context);
            } else if (null != filter) {
                JSON.render(output, result, filter, context);
            } else {
                output.append(S.string(result));
            }
        }
    },

    CSV() {
        @Override
        public void render(Output output, Object result, PropertySpec.MetaInfo spec, ActContext context) {
            if (null == result) {
                return;
            }
            Iterator iterator;
            Class<?> componentType;
            if (result instanceof Iterable) {
                iterator = ((Iterable) result).iterator();
            } else if (result instanceof Iterator) {
                iterator = (Iterator) result;
            } else if (result instanceof Enumeration) {
                final Enumeration enumeration = (Enumeration) result;
                iterator = new Iterator() {
                    @Override
                    public boolean hasNext() {
                        return enumeration.hasMoreElements();
                    }

                    @Override
                    public Object next() {
                        return enumeration.nextElement();
                    }

                    @Override
                    public void remove() {
                        throw E.unsupport();
                    }
                };
            } else {
                iterator = C.list(result).iterator();
            }
            Object firstElement = iterator.hasNext() ? iterator.next() : null;
            if (null == firstElement) {
                return;
            }
            componentType = firstElement.getClass();
            DataPropertyRepository repo = context.app().service(DataPropertyRepository.class);
            spec = PropertySpec.MetaInfo.withCurrent(spec, context);
            if (null == spec) {
                spec = new PropertySpec.MetaInfo();
                spec.onValue("-not_exists");
            }
            List<String> outputFields = repo.outputFields(spec, componentType, context);
            output.append(buildHeaderLine(outputFields, spec.labelMapping()));
            output.append($.OS.lineSeparator());
            output.append(buildDataLine(firstElement, outputFields));
            while (iterator.hasNext()) {
                output.append($.OS.lineSeparator());
                output.append(buildDataLine(iterator.next(), outputFields));
                output.flush();
            }
        }

        private String buildDataLine(Object data, List<String> outputFields) {
            Iterator<String> itr = outputFields.iterator();
            String prop = itr.next();
            S.Buffer buf = S.buffer();
            buf.append(getProperty(data, prop));
            while (itr.hasNext()) {
                buf.append(",").append(getProperty(data, itr.next()));
            }
            return buf.toString();
        }

        private String getProperty(Object data, String prop) {
            if ("this".equals(prop)) {
                return (escape(data));
            } else {
                if (data instanceof AdaptiveRecord) {
                    return escape(S.string(((AdaptiveRecord) data).getValue(prop)));
                }
                return escape($.getProperty(data, prop));
            }
        }

        private String buildHeaderLine(List<String> outputFields, Map<String, String> labels) {
            if (null == labels) {
                labels = new HashMap<>();
            }
            Iterator<String> itr = outputFields.iterator();
            String label = label(itr.next(), labels);
            S.Buffer buf = S.buffer();
            buf.append(label);
            while (itr.hasNext()) {
                buf.append(",").append(escape(label(itr.next(), labels)));
            }
            return buf.toString();
        }

        private String label(String key, Map<String, String> labels) {
            String s = labels.get(key);
            return null == s ? key : s;
        }

        private String escape(Object o) {
            if (null == o) {
                return "";
            }
            String s = o.toString().trim();
            if (s.startsWith("\"") && s.endsWith("\"")) {
                return s;
            }
            return Escape.CSV.apply(o).toString();
        }

    };

    public void render(Output out, Object result, PropertySpec.MetaInfo spec, ActContext context) {
        throw E.unsupport();
    }

    public final String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
        S.Buffer sb = S.buffer();
        render(sb, result, spec, context);
        return sb.toString();
    }

    public void print(Object result, PropertySpec.MetaInfo spec, CliContext context) {
        context.println(render(result, spec, context));
    }

    protected List toList(Object result) {
        List dataList;
        if (result instanceof Iterable) {
            dataList = C.list((Iterable) result);
        } else if (result instanceof Iterator) {
            dataList = C.list((Iterator) result);
        } else if (result instanceof Enumeration) {
            dataList = C.list((Enumeration) result);
        } else {
            dataList = C.listOf(result);
        }
        return dataList;
    }

}
