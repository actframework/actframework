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

import act.Act;
import act.cli.CliContext;
import act.cli.CliOverHttpContext;
import act.cli.ascii_table.impl.CollectionASCIITableAware;
import act.cli.tree.TreeNode;
import act.cli.util.CliCursor;
import act.cli.util.MappedFastJsonNameFilter;
import act.cli.util.TableCursor;
import act.data.DataPropertyRepository;
import act.util.ActContext;
import act.util.DisableFastJsonCircularReferenceDetect;
import act.util.FastJsonPropertyPreFilter;
import act.util.PropertySpec;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
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
        public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {

            if (null == result) {
                return "no data";
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
                return "";
            }
            Class<?> componentType;
            if (dataList.isEmpty()) {
                return "no data";
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
            return S.concat(tableString, "Items found: ", S.string(itemsFound), appendix);
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
        public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
            if (result instanceof TreeNode) {
                return toTreeString((TreeNode) result);
            } else if (result instanceof Iterable) {
                return TABLE.render(result, spec, context);
            } else if (null != spec) {
                return JSON.render(result, spec, context);
            } else {
                return S.string(result);
            }
        }

        private String toTreeString(TreeNode result) {
            StringBuilder sb = S.newBuilder();
            buildTree(sb, result, "", true);
            return sb.toString();
        }

        private void buildTree(StringBuilder sb, TreeNode node, String prefix, boolean isTrail) {
            StringBuilder sb0 = S.newBuilder().append(prefix).append(isTrail ? "└── " : "├── ").append(node.label()).append("\n");
            sb.append(sb0);
            List<TreeNode> children = node.children();
            int sz = children.size();
            if (sz == 0) {
                return;
            }
            final String subPrefix = S.newBuilder().append(prefix).append(isTrail ? "    " : "│   ").toString();
            for (int i = 0; i < sz - 1; ++i) {
                TreeNode child = children.get(i);
                buildTree(sb, child, subPrefix, false);
            }
            buildTree(sb, children.get(sz - 1), subPrefix, true);
        }
    },

    /**
     * Present the result using {@link act.cli.XmlView}
     */
    XML() {
        @Override
        public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
            throw E.unsupport();
        }
    },

    /**
     * Present the result using {@link act.cli.JsonView}
     */
    JSON() {
        @Override
        public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
            return render(result, spec, context, context instanceof CliContext);
        }

        public String render(Object result, PropertySpec.MetaInfo spec, ActContext context, boolean format) {
            String json;
            FastJsonPropertyPreFilter propertyFilter;
            spec = PropertySpec.MetaInfo.withCurrent(spec, context);
            if (null == spec) {
                propertyFilter = null;
            } else {
                propertyFilter = new FastJsonPropertyPreFilter();
                List<String> outputs = spec.outputFields(context);
                Set<String> excluded = spec.excludedFields(context);
                if (excluded.isEmpty()) {
                    if (outputs.isEmpty()) {
                        propertyFilter = null; // no filter defined actually
                    } else {
                        // output fields only applied when excluded fields not presented
                        propertyFilter.addIncludes(outputs);
                        if (FastJsonPropertyPreFilter.hasPattern(outputs)) {
                            // TODO: handle the case when result is an Iterable
                            propertyFilter.setFullPaths(context.app().service(DataPropertyRepository.class).propertyListOf(result.getClass()));
                        }
                    }
                } else {
                    propertyFilter.addExcludes(excluded);
                    if (FastJsonPropertyPreFilter.hasPattern(excluded)) {
                        // TODO: handle the case when result is an Iterable
                        propertyFilter.setFullPaths(context.app().service(DataPropertyRepository.class).propertyListOf(result.getClass()));
                    }
                }
            }

            List<SerializerFeature> featureList = C.newList();
            if (format) {
                featureList.add(SerializerFeature.PrettyFormat);
            }
            if (null == propertyFilter) {
                Boolean b = DisableFastJsonCircularReferenceDetect.option.get();
                if (null != b && b) {
                    featureList.add(SerializerFeature.DisableCircularReferenceDetect);
                }
                SerializerFeature[] featureArray = new SerializerFeature[featureList.size()];
                featureArray = featureList.toArray(featureArray);
                if (format) {
                    json = com.alibaba.fastjson.JSON.toJSONString(result, featureArray);
                } else {
                    json = com.alibaba.fastjson.JSON.toJSONString(result);
                }
            } else {
                Boolean b = DisableFastJsonCircularReferenceDetect.option.get();
                if (null != b && b) {
                    Act.LOGGER.warn(new RuntimeException(), "Cannot use @DisableFastJsonCircularReferenceDetect along with @PropertySpec");
                    // Note: we can't check DisableFastJsonCircularReferenceDetect here because if
                    // that option is set, then FastJson will skip the JsonSerializer.context setting
                    // and breaks the property filter mechanism
                    //featureList.add(SerializerFeature.DisableCircularReferenceDetect);
                }
                MappedFastJsonNameFilter nameFilter = new MappedFastJsonNameFilter(spec.labelMapping(context));

                SerializerFeature[] featureArray = new SerializerFeature[featureList.size()];
                featureArray = featureList.toArray(featureArray);

                if (nameFilter.isEmpty()) {
                    json = com.alibaba.fastjson.JSON.toJSONString(result, propertyFilter, featureArray);
                } else {
                    SerializeFilter[] filters = new SerializeFilter[2];
                    filters[0] = nameFilter;
                    filters[1] = propertyFilter;
                    json = com.alibaba.fastjson.JSON.toJSONString(result, filters, featureArray);
                }
            }
            return json;
        }


    },

    /**
     * Present the result using {@link Object#toString()}
     */
    TO_STRING() {
        @Override
        public String render(Object result, PropertySpec.MetaInfo filter, ActContext context) {
            if (result instanceof Iterable) {
                return TABLE.render(result, filter, context);
            } else if (result instanceof TreeNode) {
                return TREE.render(result, filter, context);
            } else if (null != filter) {
                return JSON.render(result, filter, context);
            } else {
                return S.string(result);
            }
        }
    },

    CSV() {
        @Override
        public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
            if (null == result) {
                return "no data";
            }
            List dataList;
            Class<?> componentType;
            if (result instanceof Iterable) {
                dataList = C.list((Iterable) result);
            } else if (result instanceof Iterator) {
                dataList = C.list((Iterator) result);
            } else if (result instanceof Enumeration) {
                dataList = C.list((Enumeration) result);
            } else {
                dataList = C.list(result);
            }
            if (dataList.isEmpty()) {
                return "no data";
            }
            componentType = dataList.get(0).getClass();
            DataPropertyRepository repo = context.app().service(DataPropertyRepository.class);
            spec = PropertySpec.MetaInfo.withCurrent(spec, context);
            if (null == spec) {
                spec = new PropertySpec.MetaInfo();
                spec.onValue("-not_exists");
            }
            List<String> outputFields = repo.outputFields(spec, componentType, context);
            S.Buffer sb = S.buffer();
            buildHeaderLine(sb, outputFields, spec.labelMapping());
            for (Object entity : dataList) {
                sb.append($.OS.lineSeparator());
                buildDataLine(sb, entity, outputFields);
            }
            return sb.toString();
        }

        private void buildDataLine(S.Buffer sb, Object data, List<String> outputFields) {
            Iterator<String> itr = outputFields.iterator();
            String prop = itr.next();
            sb.append(getProperty(data, prop));
            while (itr.hasNext()) {
                sb.append(",").append(getProperty(data, itr.next()));
            }
        }

        private String getProperty(Object data, String prop) {
            if ("this".equals(prop)) {
                return (escape(data));
            } else {
                return escape($.getProperty(data, prop));
            }
        }

        private void buildHeaderLine(S.Buffer sb, List<String> outputFields, Map<String, String> labels) {
            if (null == labels) {
                labels = C.newMap();
            }
            Iterator<String> itr = outputFields.iterator();
            String label = label(itr.next(), labels);
            sb.append(label);
            while (itr.hasNext()) {
                sb.append(",").append(escape(label(itr.next(), labels)));
            }
        }

        private String label(String key, Map<String, String> labels) {
            String s = labels.get(key);
            return null == s ? key : s;
        }

        private String escape(Object o) {
            return Escape.CSV.apply(o).toString();
        }

    };

    public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
        throw E.unsupport();
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
