package act.cli.meta;

import act.Act;
import act.Destroyable;
import act.app.App;
import act.app.AppClassLoader;
import act.app.CliContext;
import act.asm.Type;
import act.cli.ascii_table.impl.CollectionASCIITableAware;
import act.cli.tree.TreeNode;
import act.cli.util.MappedFastJsonNameFilter;
import act.data.DataPropertyRepository;
import act.handler.CliHandler;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.FastJsonPropertyPreFilter;
import act.util.PropertySpec;
import act.util.DestroyableBase;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Stores the command method meta info including
 * <ul>
 *     <li>method name</li>
 *     <li>method invocation type</li>
 *     <li>return type</li>
 *     <li>param info and associated annotation info</li>
 * </ul>
 */
public class CommandMethodMetaInfo extends DestroyableBase {

    /**
     * Define how the command method return result should
     * be presented
     */
    public static enum View {
        /**
         * present the result using {@link act.cli.TableView}
         */
        TABLE () {
            @Override
            @SuppressWarnings("unchecked")
            public String render(Object result, PropertySpec.MetaInfo filter, CliContext context) {
                if (null == filter) {
                    // TODO: support Table View when filter annotation is not presented
                    return TO_STRING.render(result, null);
                }
                List dataList;
                boolean isList = false;
                if (result instanceof Iterable) {
                    dataList = C.list((Iterable) result);
                    isList = true;
                } else {
                    dataList = C.listOf(result);
                }

                Set<String> excluded = filter.excludedFields();
                List<String> outputFields = filter.outputFields();
                if (!excluded.isEmpty()) {
                    DataPropertyRepository repo = App.instance().service(DataPropertyRepository.class);
                    Class resultClass = result.getClass();
                    if (isList) {
                        if (dataList.isEmpty()) {
                            return "no data found";
                        }
                        resultClass = dataList.get(0).getClass();
                    }
                    List<String> allFields = repo.propertyListOf(resultClass);
                    outputFields = C.list(allFields).without(excluded);
                }
                return context.getTable(new CollectionASCIITableAware(dataList, outputFields, filter.labels(outputFields)));
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
            public String render(Object result, PropertySpec.MetaInfo spec, CliContext context) {
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
            public String render(Object result, PropertySpec.MetaInfo filter) {
                return render(result, filter, true);
            }

            public String render(Object result, PropertySpec.MetaInfo filter, boolean format) {
                String json;
                if (null == filter) {
                    json = com.alibaba.fastjson.JSON.toJSONString(result, SerializerFeature.PrettyFormat);
                } else {
                    FastJsonPropertyPreFilter propertyFilter = new FastJsonPropertyPreFilter();
                    List<String> outputs = filter.outputFields();
                    Set<String> excluded = filter.excludedFields();
                    if (excluded.isEmpty()) {
                        // output fields only applied when excluded fields not presented
                        propertyFilter.addIncludes(outputs);
                    } else {
                        propertyFilter.addExcludes(excluded);
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
            public String render(Object result, PropertySpec.MetaInfo filter, CliContext cliContext) {
                if (result instanceof Iterable) {
                    return TABLE.render(result, filter, cliContext);
                } else if (result instanceof TreeNode) {
                    return TREE.render(result, filter, cliContext);
                } else if (null != filter) {
                    return JSON.render(result, filter, cliContext);
                } else {
                    return S.string(result);
                }
            }
        };

        public String render(Object result, PropertySpec.MetaInfo spec) {
            throw E.unsupport();
        }

        protected String render(Object result, PropertySpec.MetaInfo spec, CliContext context) {
            return render(result, spec);
        }

        public void print(Object result, PropertySpec.MetaInfo spec, CliContext context) {
            context.println(render(result, spec, context));
        }
    }

    private String methodName;
    private String commandName;
    private String helpMsg;
    private InvokeType invokeType;
    private CommanderClassMetaInfo clsInfo;
    private PropertySpec.MetaInfo propertySpec;
    private C.List<CommandParamMetaInfo> params = C.newList();
    private ReturnTypeInfo returnType;
    private Set<String> optionLeads = C.newSet();
    private View view = View.TO_STRING;
    private Act.Mode mode = Act.Mode.PROD;

    public CommandMethodMetaInfo(CommanderClassMetaInfo clsInfo) {
        this.clsInfo = $.NPE(clsInfo);
    }

    public CommanderClassMetaInfo classInfo() {
        return clsInfo;
    }

    public CommandMethodMetaInfo methodName(String name) {
        this.methodName = $.NPE(name);
        return this;
    }

    public String methodName() {
        return methodName;
    }

    public CommandMethodMetaInfo view(View view) {
        this.view = $.notNull(view);
        return this;
    }

    public View view() {
        return view;
    }

    public CommandMethodMetaInfo commandName(String name) {
        commandName = $.NPE(name);
        return this;
    }

    public String commandName() {
        return commandName;
    }

    public String fullName() {
        return S.builder(clsInfo.className()).append(".").append(methodName()).toString();
    }

    public CommandMethodMetaInfo helpMsg(String msg) {
        helpMsg = msg;
        return this;
    }

    public String helpMsg() {
        return helpMsg;
    }

    /**
     * Returns {@link CliHandler#options()}
     * @return options list
     * @see CliHandler#options()
     */
    public List<$.T2<String, String>> options(CommanderClassMetaInfo classMetaInfo, AppClassLoader classLoader) {
        List<$.T2<String, String>> retVal = C.newList();
        for (CommandParamMetaInfo param : params) {
            OptionAnnoInfoBase opt = param.optionInfo();
            if (null != opt) {
                retVal.add($.T2(opt.leads(), opt.help()));
            }
        }
        for (OptionAnnoInfoBase opt : classMetaInfo.fieldOptionAnnoInfoList(classLoader)) {
            retVal.add($.T2(opt.leads(), opt.help()));
        }
        return retVal;
    }

    /**
     * Returns {@link act.handler.CliHandler#commandLine(String)}
     * @return the command line
     * @see act.handler.CliHandler#commandLine(String)
     */
    public $.T2<String, String> commandLine(String commandName, CommanderClassMetaInfo classMetaInfo, AppClassLoader classLoader) {
        boolean hasOptions = classMetaInfo.hasOption(classLoader);
        String firstArg = null;
        boolean hasMoreArgs = false;
        for (CommandParamMetaInfo param : params) {
            if (param.optionInfo() != null) {
                hasOptions = true;
            } else {
                if (firstArg == null) {
                    firstArg = param.name();
                } else {
                    hasMoreArgs = true;
                }
            }
        }
        StringBuilder sb = S.builder(commandName);
        if (hasOptions) {
            sb.append(" [options]");
        }
        if (null != firstArg) {
            sb.append(" ");
            sb.append("[").append(firstArg);
            if (hasMoreArgs) {
                sb.append("...");
            }
            sb.append("]");
        }
        return $.T2(sb.toString(), helpMsg());
    }

    public CommandMethodMetaInfo mode(Act.Mode mode) {
        this.mode = mode;
        return this;
    }

    public Act.Mode mode() {
        return mode;
    }

    public CommandMethodMetaInfo invokeStaticMethod() {
        invokeType = InvokeType.STATIC;
        return this;
    }

    public CommandMethodMetaInfo invokeInstanceMethod() {
        invokeType = InvokeType.VIRTUAL;
        return this;
    }

    public boolean isStatic() {
        return InvokeType.STATIC == invokeType;
    }

    public CommandMethodMetaInfo propertySpec(PropertySpec.MetaInfo propertySpec) {
        this.propertySpec = propertySpec;
        return this;
    }

    public PropertySpec.MetaInfo propertySpec() {
        return propertySpec;
    }

    public CommandMethodMetaInfo returnType(Type type) {
        returnType = ReturnTypeInfo.of(type);
        return this;
    }

    public Type returnType() {
        return returnType.type();
    }

    public CommandMethodMetaInfo addParam(CommandParamMetaInfo paramInfo) {
        params.add(paramInfo);
        return this;
    }

    public C.List<CommandParamMetaInfo> params() {
        return C.list(params);
    }

    public CommandParamMetaInfo param(int id) {
        return params.get(id);
    }

    public int paramCount() {
        return params.size();
    }

    public CommandMethodMetaInfo addLead(String lead) {
        if (null == lead) {
            return this;
        }
        if (optionLeads.contains(lead)) {
            throw E.unexpected("Duplicate option lead %s found on %s.%s", lead, clsInfo.className(), methodName);
        }
        optionLeads.add(lead);
        return this;
    }

    @Override
    protected void releaseResources() {
        super.releaseResources();
        clsInfo.destroy();
        Destroyable.Util.destroyAll(params);
    }
}
