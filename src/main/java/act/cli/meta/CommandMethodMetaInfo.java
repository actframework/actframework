package act.cli.meta;

import act.Destroyable;
import act.app.CliContext;
import act.asm.Type;
import act.cli.ascii_table.impl.CollectionASCIITableAware;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.FastJsonPropertyPreFilter;
import act.util.PropertyFilter;
import act.util.DestroyableBase;
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
            @SuppressWarnings("unchecked")
            public void print(Object result, PropertyFilter.MetaInfo filter, CliContext context) {
                if (null == filter) {
                    // TODO: support Table View when filter annotation is not presented
                    TO_STRING.print(result, null, context);
                    return;
                }
                List dataList;
                if (result instanceof Iterable) {
                    dataList = C.list((Iterable) result);
                } else {
                    dataList = C.listOf(result);
                }
                context.printTable(new CollectionASCIITableAware(dataList, filter.outputFields(), filter.labels()));
            }
        },

        /**
         * Present the result using {@link act.cli.JsonView}
         */
        JSON () {
            @Override
            public void print(Object result, PropertyFilter.MetaInfo filter, CliContext context) {
                String json;
                if (null == filter) {
                    json = com.alibaba.fastjson.JSON.toJSONString(result, SerializerFeature.PrettyFormat);
                } else {
                    FastJsonPropertyPreFilter f = new FastJsonPropertyPreFilter();
                    f.addIncludes(filter.outputFields());
                    f.addExcludes(filter.excludedFields());
                    json = com.alibaba.fastjson.JSON.toJSONString(result, f, SerializerFeature.PrettyFormat);
                }
                // TODO: handle labels in JSON serialization
                context.println(json);
            }
        },

        /**
         * Present the result using {@link Object#toString()}
         */
        TO_STRING () {
            @Override
            public void print(Object result, PropertyFilter.MetaInfo filter, CliContext context) {
                if (null != filter) {
                    // if PropertyFilter annotation presented, then by default
                    // use the JSON view to print the result
                    JSON.print(result, filter, context);
                } else {
                    context.println(result.toString());
                }
            }
        };

        public abstract void print(Object result, PropertyFilter.MetaInfo filter, CliContext context);
    }

    private String methodName;
    private String commandName;
    private String helpMsg;
    private InvokeType invokeType;
    private CommanderClassMetaInfo clsInfo;
    private PropertyFilter.MetaInfo dataView;
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

    public CommandMethodMetaInfo dataView(PropertyFilter.MetaInfo dataView) {
        this.dataView = dataView;
        return this;
    }

    public PropertyFilter.MetaInfo dataViewInfo() {
        return dataView;
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
