package act.cli.meta;

import act.Destroyable;
import act.app.App;
import act.app.CliContext;
import act.asm.Type;
import act.cli.ascii_table.impl.CollectionASCIITableAware;
import act.cli.util.MappedFastJsonNameFilter;
import act.data.DataPropertyRepository;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.FastJsonPropertyPreFilter;
import act.util.PropertySpec;
import act.util.DestroyableBase;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

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
            public void print(Object result, PropertySpec.MetaInfo spec, CliContext context) {
                context.println(render(result, spec, context));
            }

            @Override
            @SuppressWarnings("unchecked")
            public String render(Object result, PropertySpec.MetaInfo filter, CliContext context) {
                if (null == filter) {
                    // TODO: support Table View when filter annotation is not presented
                    return TO_STRING.render(result, null);
                }
                List dataList;
                if (result instanceof Iterable) {
                    dataList = C.list((Iterable) result);
                } else {
                    dataList = C.listOf(result);
                }

                Set<String> excluded = filter.excludedFields();
                List<String> outputFields = filter.outputFields();
                if (!excluded.isEmpty()) {
                    DataPropertyRepository repo = App.instance().service(DataPropertyRepository.class);
                    List<String> allFields = repo.propertyListOf(result.getClass());
                    outputFields = C.list(allFields).without(excluded);
                }
                return context.getTable(new CollectionASCIITableAware(dataList, outputFields, filter.labels(outputFields)));
            }
        },

        /**
         * Present the result using {@link act.cli.JsonView}
         */
        JSON () {
            @Override
            public String render(Object result, PropertySpec.MetaInfo filter) {
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
                        json = com.alibaba.fastjson.JSON.toJSONString(result, propertyFilter, SerializerFeature.PrettyFormat);
                    } else {
                        SerializeFilter[] filters = new SerializeFilter[2];
                        filters[0] = nameFilter;
                        filters[1] = propertyFilter;
                        json = com.alibaba.fastjson.JSON.toJSONString(result, filters, SerializerFeature.PrettyFormat);
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
            public String render(Object result, PropertySpec.MetaInfo filter) {
                if (null != filter) {
                    // if PropertySpec annotation presented, then by default
                    // use the JSON view to print the result
                    return JSON.render(result, filter);
                } else {
                    return (result.toString());
                }
            }
        };

        public String render(Object result, PropertySpec.MetaInfo spec) {
            throw E.unsupport();
        }

        protected String render(Object result, PropertySpec.MetaInfo spec, CliContext context) {
            throw E.unsupport();
        }

        public void print(Object result, PropertySpec.MetaInfo spec, CliContext context) {
            context.println(render(result, spec));
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

    public String help() {
        StringBuilder sb = S.builder().append(commandName);
        if (S.notBlank(helpMsg)) {
            sb.append("\t").append(helpMsg);
        }
        for (CommandParamMetaInfo param : params) {
            OptionAnnoInfo opt = param.optionInfo();
            if (null != opt) {
                sb.append(opt.help());
            }
        }
        return sb.toString();
    }

    @Override
    protected void releaseResources() {
        super.releaseResources();
        clsInfo.destroy();
        Destroyable.Util.destroyAll(params);
    }
}
