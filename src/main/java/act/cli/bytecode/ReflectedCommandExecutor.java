package act.cli.bytecode;

import act.app.App;
import act.app.CliContext;
import act.app.data.StringValueResolverManager;
import act.cli.CliError;
import act.cli.CommandExecutor;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommandParamMetaInfo;
import act.cli.meta.OptionAnnoInfo;
import act.cli.util.CommandLineParser;
import act.conf.AppConfig;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Implement {@link act.cli.CommandExecutor} using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedCommandExecutor extends CommandExecutor {

    private CommandMethodMetaInfo meta;
    private App app;
    private ClassLoader cl;
    private Class[] paramTypes;
    private Class<?> commanderClass;
    private Method method;
    private MethodAccess methodAccess;
    private int commandIndex;

    public ReflectedCommandExecutor(CommandMethodMetaInfo meta, App app) {
        this.meta = $.NPE(meta);
        this.app = $.NPE(app);
        this.cl = app.classLoader();
        this.paramTypes = paramTypes();
        this.commanderClass = $.classForName(meta.classInfo().className(), cl);
        if (!meta.isStatic()) {
            methodAccess = MethodAccess.get(commanderClass);
            commandIndex = methodAccess.getIndex(meta.methodName(), paramTypes);
        } else {
            try {
                method = commanderClass.getMethod(meta.methodName(), paramTypes);
            } catch (NoSuchMethodException e) {
                throw E.unexpected(e);
            }
            method.setAccessible(true);
        }
    }

    @Override
    public Object execute(CliContext context) {
        Object cmd = commanderInstance(context);
        Object[] params = params(context);
        return invoke(context, cmd, params);
    }

    @Override
    protected void releaseResources() {
        app = null;
        cl = null;
        commandIndex = 0;
        commanderClass = null;
        method = null;
        methodAccess = null;
        paramTypes = null;
        super.releaseResources();
    }

    private Object commanderInstance(CliContext context) {
        String commander = commanderClass.getName();
        Object inst = context.__commanderInstance(commander);
        if (null == inst) {
            inst = context.newInstance(commanderClass);
            context.__commanderInstance(commander, inst);
        }
        return inst;
    }

    private Class<?>[] paramTypes() {
        int paramCount = meta.paramCount();
        Class<?>[] ca = new Class[paramCount];
        if (0 == paramCount) {
            return ca;
        }
        for (int i = 0; i < paramCount; ++i) {
            CommandParamMetaInfo param = meta.param(i);
            String className = param.type().getClassName();
            ca[i] = $.classForName(className, cl);
        }
        return ca;
    }

    private Object[] params(CliContext ctx) {
        int paramCount = meta.paramCount();
        Object[] oa = new Object[paramCount];
        if (0 == paramCount) {
            return oa;
        }
        StringValueResolverManager resolverManager = app.resolverManager();

        List<String> args = ctx.arguments();
        CommandLineParser parser = ctx.commandLine();
        int argIdx = 0;
        for (int i = 0; i < paramCount; ++i) {
            CommandParamMetaInfo param = meta.param(i);
            Class<?> paramType = paramTypes[i];
            if (CliContext.class.equals(paramType)) {
                oa[i] = ctx;
            } else if (App.class.equals(paramType)) {
                oa[i] = ctx.app();
            } else if (AppConfig.class.equals(paramType)) {
                oa[i] = ctx.app().config();
            } else {
                String argStr;
                OptionAnnoInfo option = param.optionInfo();
                if (null == option) {
                    argStr = args.get(argIdx++);
                } else {
                    argStr = parser.getString(option.lead1(), option.lead2());
                    if (S.blank(argStr) && option.required()) {
                        throw new CliError("Miss required option " + option.leads());
                    }
                }
                oa[i] = resolverManager.resolve(argStr, paramType);
            }
        }
        return oa;
    }

    private Object invoke(CliContext context, Object commander, Object[] params) {
        Object result;
        if (null != methodAccess) {
            try {
                result = methodAccess.invoke(commander, commandIndex, params);
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        } else {
            try {
                result = method.invoke(null, params);
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
        return result;
    }


}
