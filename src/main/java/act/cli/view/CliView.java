package act.cli.view;

import act.app.CliContext;
import act.cli.ascii_table.impl.CollectionASCIITableAware;
import act.cli.tree.TreeNode;
import act.cli.util.MappedFastJsonNameFilter;
import act.data.DataPropertyRepository;
import act.util.ActContext;
import act.util.FastJsonPropertyPreFilter;
import act.util.PropertySpec;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;
import java.util.Set;

/**
 * Define how the command method return result should
 * be presented
 */
public enum CliView {
    /**
     * present the result using {@link act.cli.TableView}
     */
    TABLE () {

        @Override
        @SuppressWarnings("unchecked")
        public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {

            if (null == result) {
                return "no data";
            }

            if (null == spec) {
                // TODO: support Table View when filter annotation is not presented
                return TO_STRING.render(result, null, context);
            }

            if (!(context instanceof CliContext)) {
                throw E.unsupport("TableView support in CliContext only. You context: ", context.getClass());
            }

            CliContext cliContext = (CliContext) context;

            List dataList;
            Class<?> componentType;
            if (result instanceof Iterable) {
                dataList = C.list((Iterable) result);
                if (dataList.isEmpty()) {
                    return "no data";
                }
                componentType = dataList.get(0).getClass();
            } else {
                dataList = C.listOf(result);
                componentType = result.getClass();
            }
            DataPropertyRepository repo = context.app().service(DataPropertyRepository.class);
            List<String> outputFields = repo.outputFields(spec, componentType, context);
            return cliContext.getTable(new CollectionASCIITableAware(dataList, outputFields, spec.labels(outputFields, context)));
        }

    },

    /**
     * Present data in a Tree structure.
     * <p>
     *     Note the {@code result} parameter must be a root {@link act.cli.tree.TreeNode node} of the tree,
     *     otherwise the data will be presented in
     * </p>
     * <ul>
     *     <li>{@link #TABLE Table view} if the result is an {@link Iterable}, or</li>
     *     <li>{@link #JSON JSON view} otherwise</li>
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
            StringBuilder sb = S.builder();
            buildTree(sb, result, "", true);
            return sb.toString();
        }

        private void buildTree(StringBuilder sb, TreeNode node, String prefix, boolean isTrail) {
            StringBuilder sb0 = S.builder(prefix).append(isTrail ? "└── " : "├── ").append(node.label()).append("\n");
            sb.append(sb0);
            List<TreeNode> children = node.children();
            int sz = children.size();
            if (sz == 0) {
                return;
            }
            final String subPrefix = S.builder(prefix).append(isTrail ? "    " : "│   ").toString();
            for (int i = 0; i < sz - 1; ++i) {
                TreeNode child = children.get(i);
                buildTree(sb, child, subPrefix, false);
            }
            buildTree(sb, children.get(sz - 1), subPrefix, true);
        }
    },

    /**
     * Present the result using {@link act.cli.JsonView}
     */
    JSON () {
        @Override
        public String render(Object result, PropertySpec.MetaInfo filter, ActContext context) {
            return render(result, filter, context, context instanceof CliContext);
        }

        public String render(Object result, PropertySpec.MetaInfo filter, ActContext context, boolean format) {
            String json;
            if (null == filter) {
                json = com.alibaba.fastjson.JSON.toJSONString(result, SerializerFeature.PrettyFormat);
            } else {
                FastJsonPropertyPreFilter propertyFilter = new FastJsonPropertyPreFilter();
                List<String> outputs = filter.outputFields(context);
                Set<String> excluded = filter.excludedFields(context);
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

                MappedFastJsonNameFilter nameFilter = new MappedFastJsonNameFilter(filter.labelMapping());

                if (nameFilter.isEmpty()) {
                    if (format) {
                        json = com.alibaba.fastjson.JSON.toJSONString(result, propertyFilter, SerializerFeature.PrettyFormat);
                    } else {
                        json = com.alibaba.fastjson.JSON.toJSONString(result, propertyFilter);
                    }
                } else {
                    SerializeFilter[] filters = new SerializeFilter[2];
                    filters[0] = nameFilter;
                    filters[1] = propertyFilter;
                    if (format) {
                        json = com.alibaba.fastjson.JSON.toJSONString(result, filters, SerializerFeature.PrettyFormat);
                    } else {
                        json = com.alibaba.fastjson.JSON.toJSONString(result, filters);
                    }
                }
            }
            return json;
        }
    },

    /**
     * Present the result using {@link Object#toString()}
     */
    TO_STRING () {
        @Override
        public String render(Object result, PropertySpec.MetaInfo filter, ActContext context) {
            if (result instanceof Iterable && null != filter) {
                return TABLE.render(result, filter, context);
            } else if (result instanceof TreeNode) {
                return TREE.render(result, filter, context);
            } else if (null != filter) {
                return JSON.render(result, filter, context);
            } else {
                return S.string(result);
            }
        }
    };

    public String render(Object result, PropertySpec.MetaInfo spec, ActContext context) {
        throw E.unsupport();
    }

    public void print(Object result, PropertySpec.MetaInfo spec, CliContext context) {
        context.println(render(result, spec, context));
    }
}
