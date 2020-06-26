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
import act.util.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.osgl.$;
import org.osgl.util.*;
import org.rythmengine.utils.Escape;
import org.w3c.dom.Document;

import java.io.Writer;
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
        public void render(Writer writer, Object result, PropertySpec.MetaInfo spec, ActContext context) {

            if (null == result) {
                return;
            }

            spec = PropertySpec.MetaInfo.withCurrentNoConsume(spec, context);

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
            List<S.Pair> outputFields = repo.outputFields(spec, componentType, dataList.get(0), context);
            if (outputFields.isEmpty()) {
                outputFields = C.<S.Pair>list(S.pair("this as Item", "this as Item"));
            }
            String tableString = cliContext.getTable(new CollectionASCIITableAware(dataList, DataPropertyRepository.getFields(outputFields), spec.labels2(outputFields, context)));
            int itemsFound = dataList.size();
            CliCursor cursor = cliContext.session().cursor();
            String appendix = "";
            if (null != cursor) {
                itemsFound = cursor.records();
                appendix = cursor.hasNext() ? "\nType \"it\" for more" : "";
            }
            IO.write(S.concat(tableString, "Items found: ", S.string(itemsFound), appendix), writer);
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
        public void render(Writer writer, Object result, PropertySpec.MetaInfo spec, ActContext context) {
            if (result instanceof TreeNode) {
                toTreeString(writer, (TreeNode) result);
            } else if (result instanceof Iterable) {
                TABLE.render(writer, result, spec, context);
            } else if (null != spec) {
                JSON.render(writer, result, spec, context);
            } else {
                IO.write(S.string(result), writer);
            }
        }

        private void toTreeString(Writer writer, TreeNode result) {
            buildTree(writer, result, "", true);
        }

        private void buildTree(Writer writer, TreeNode node, String prefix, boolean isTrail) {
            StringBuilder sb = S.newBuilder().append(prefix).append(isTrail ? "└── " : "├── ").append(node.label()).append("\n");
            IO.write(sb, writer, false);
            List<TreeNode> children = node.children();
            int sz = children.size();
            if (sz == 0) {
                return;
            }
            final String subPrefix = S.newBuilder().append(prefix).append(isTrail ? "    " : "│   ").toString();
            for (int i = 0; i < sz - 1; ++i) {
                TreeNode child = children.get(i);
                buildTree(writer, child, subPrefix, false);
            }
            buildTree(writer, children.get(sz - 1), subPrefix, true);
        }
    },

    /**
     * Present the result using {@link act.cli.XmlView}
     */
    XML() {
        @Override
        public void render(Writer writer, Object result, PropertySpec.MetaInfo spec, ActContext context) {
            Class<?> mappedResultType = (result instanceof Iterable) ? JSONArray.class : JSONObject.class;
            Object mappedResult;
            if (null != spec) {
                mappedResult = spec.applyTo($.map(result), context).to(mappedResultType);
            } else {
                mappedResult = $.map(result).to(mappedResultType);
            }
            IO.write($.convert(mappedResult).to(Document.class)).to(writer);
        }
    },

    /**
     * Present the result using {@link act.util.JsonView}
     */
    JSON() {
        @Override
        public void render(Writer writer, Object result, PropertySpec.MetaInfo spec, ActContext context) {
            render(writer, result, spec, context, context instanceof CliContext);
        }

        public void render(Writer writer, Object result, PropertySpec.MetaInfo spec, ActContext context, boolean format) {
            new JsonUtilConfig.JsonWriter(result, spec, format, context).apply(writer);
        }
    },

    /**
     * Present the result using {@link Object#toString()}
     */
    TO_STRING() {
        @Override
        public void render(Writer writer, Object result, PropertySpec.MetaInfo filter, ActContext context) {
            if (result instanceof Iterable) {
                TABLE.render(writer, result, filter, context);
            } else if (result instanceof TreeNode) {
                TREE.render(writer, result, filter, context);
            } else if (null != filter) {
                JSON.render(writer, result, filter, context);
            } else {
                IO.write(S.string(result), writer);
            }
        }
    },

    CSV() {
        @Override
        public void render(Writer writer, Object result, PropertySpec.MetaInfo spec, ActContext context) {
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
            boolean hasElement = iterator.hasNext();
            Object firstElement = hasElement ? iterator.next() : null;
            if (null == firstElement) {
                return;
            }
            componentType = firstElement.getClass();
            DataPropertyRepository repo = context.app().service(DataPropertyRepository.class);
            spec = PropertySpec.MetaInfo.withCurrentNoConsume(spec, context);
            if (null == spec) {
                spec = new PropertySpec.MetaInfo();
                spec.onValue("-not_exists");
            }
            List<S.Pair> outputFields = repo.outputFields(spec, componentType, firstElement, context);
            if (outputFields.isEmpty()) {
                return;
            }
            IO.write(buildHeaderLine(outputFields, spec.labelMapping(context)), writer, false);
            IO.write($.OS.lineSeparator(), writer, false);
            IO.write(buildDataLine(firstElement, outputFields), writer, false);
            boolean isCli = context instanceof CliContext;
            while (iterator.hasNext()) {
                IO.write($.OS.lineSeparator(), writer, false);
                IO.write(buildDataLine(iterator.next(), outputFields), writer, false);
                if (isCli) {
                    IO.flush(writer);
                }
            }
        }

        private String buildDataLine(Object data, List<S.Pair> outputFields) {
            Iterator<S.Pair> itr = outputFields.iterator();
            S.Pair prop = itr.next();
            S.Buffer buf = S.buffer();
            buf.append(getProperty(data, prop));
            while (itr.hasNext()) {
                buf.append(",").append(getProperty(data, itr.next()));
            }
            return buf.toString();
        }

        private String getProperty(Object data, S.Pair prop) {
            if ("this".equals(prop._1)) {
                return (escape(data));
            } else {
                if (data instanceof AdaptiveMap) {
                    return escape(S.string(((AdaptiveMap) data).getValue(prop._1)));
                } else if (data instanceof Map) {
                    return escape(S.string(((Map) data).get(prop._1)));
                }
                return escape($.getProperty(data, prop._1));
            }
        }

        private String buildHeaderLine(List<S.Pair> outputFields, Map<String, String> labels) {
            if (null == labels) {
                labels = new HashMap<>();
            }
            Iterator<S.Pair> itr = outputFields.iterator();
            String label = label(itr.next(), labels);
            S.Buffer buf = S.buffer();
            buf.append(label);
            while (itr.hasNext()) {
                buf.append(",").append(escape(label(itr.next(), labels)));
            }
            return buf.toString();
        }

        private String label(S.Pair pair, Map<String, String> labels) {
            String s = labels.get(pair._1);
            String defLabel = pair._2;
            if (null == defLabel) {
                defLabel = pair._1;
            }
            return null == s ? defLabel : s;
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

    public void render(Writer writer, Object result, PropertySpec.MetaInfo spec, ActContext context) {
        throw E.unsupport();
    }

    public final String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
        S.Buffer sb = S.buffer();
        render($.convert(sb).to(Writer.class), result, spec, context);
        return sb.toString();
    }

    public void print(Object result, PropertySpec.MetaInfo spec, CliContext context) {
        context.println(render(result, spec, context));
    }

    protected List toList(Object result) {
        List dataList;
        if (result instanceof List) {
            return (List) result;
        } else if (result instanceof Iterable) {
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
