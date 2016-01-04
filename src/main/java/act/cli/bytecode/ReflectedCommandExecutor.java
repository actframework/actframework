package act.cli.bytecode;

import act.app.App;
import act.app.CliContext;
import act.app.data.StringValueResolverManager;
import act.cli.CliError;
import act.cli.CommandExecutor;
import act.cli.meta.*;
import act.cli.util.CommandLineParser;
import act.conf.AppConfig;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Implement {@link act.cli.CommandExecutor} using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedCommandExecutor extends CommandExecutor {

    private CommanderClassMetaInfo classMetaInfo;
    private CommandMethodMetaInfo methodMetaInfo;
    private App app;
    private ClassLoader cl;
    private Class[] paramTypes;
    private Class<?> commanderClass;
    private Method method;
    private MethodAccess methodAccess;
    private int commandIndex;

    public ReflectedCommandExecutor(CommanderClassMetaInfo classMetaInfo, CommandMethodMetaInfo methodMetaInfo, App app) {
        this.classMetaInfo = $.notNull(classMetaInfo);
        this.methodMetaInfo = $.notNull(methodMetaInfo);
        this.app = $.NPE(app);
        this.cl = app.classLoader();
        this.paramTypes = paramTypes();
        this.commanderClass = $.classForName(methodMetaInfo.classInfo().className(), cl);
        if (!methodMetaInfo.isStatic()) {
            methodAccess = MethodAccess.get(commanderClass);
            commandIndex = methodAccess.getIndex(methodMetaInfo.methodName(), paramTypes);
        } else {
            try {
                method = commanderClass.getMethod(methodMetaInfo.methodName(), paramTypes);
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
        return invoke(cmd, params);
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
        $.Var<Integer> argIdx = $.var(0);
        List<FieldOptionAnnoInfo> list = classMetaInfo.fieldOptionAnnoInfoList();
        for (FieldOptionAnnoInfo fieldOptionAnnoInfo : list) {
            String fieldName = fieldOptionAnnoInfo.fieldName();
            Object val = optionVal(fieldOptionAnnoInfo.fieldType(), fieldOptionAnnoInfo, argIdx, false, context);
            Class instClass = inst.getClass();
            try {
                Field field = instClass.getField(fieldName);
                field.setAccessible(true);
                field.set(val, inst);
            } catch (Exception e) {
                try {
                    Method method = instClass.getMethod("set" + S.capFirst(fieldName), fieldOptionAnnoInfo.fieldType());
                    method.invoke(inst, val);
                } catch (Exception e1) {
                    throw E.unexpected("Cannot find the setter for field %s on class %s", fieldName, instClass);
                }
            }
        }
        return inst;
    }

    private Class<?>[] paramTypes() {
        int paramCount = methodMetaInfo.paramCount();
        Class<?>[] ca = new Class[paramCount];
        if (0 == paramCount) {
            return ca;
        }
        for (int i = 0; i < paramCount; ++i) {
            CommandParamMetaInfo param = methodMetaInfo.param(i);
            String className = param.type().getClassName();
            ca[i] = $.classForName(className, cl);
        }
        return ca;
    }

    private Object[] params(CliContext ctx) {
        int paramCount = methodMetaInfo.paramCount();
        Object[] oa = new Object[paramCount];
        if (0 == paramCount) {
            return oa;
        }
        $.Var<Integer> argIdx = $.var(0);
        for (int i = 0; i < paramCount; ++i) {
            CommandParamMetaInfo param = methodMetaInfo.param(i);
            Class<?> paramType = paramTypes[i];
            oa[i] = optionVal(paramType, param.optionInfo(), argIdx, paramCount == 1, ctx);
        }
        return oa;
    }

    private Object optionVal(Class<?> optionType, OptionAnnoInfoBase option, $.Var<Integer> argIdx, boolean useArgumentIfOptionNotFound, CliContext ctx) {
        StringValueResolverManager resolverManager = app.resolverManager();
        CommandLineParser parser = ctx.commandLine();
        List<String> args = ctx.arguments();
        if (CliContext.class.equals(optionType)) {
            return ctx;
        } else if (App.class.equals(optionType)) {
            return ctx.app();
        } else if (AppConfig.class.equals(optionType)) {
            return ctx.app().config();
        } else {
            String argStr;
            if (null == option) {
                int i = argIdx.get();
                argStr = args.get(i);
                argIdx.set(i + 1);
            } else {
                argStr = parser.getString(option.lead1(), option.lead2());
                if (S.blank(argStr) && option.required()) {
                    if (useArgumentIfOptionNotFound) {
                        // try to use the single param as the option
                        List<String> args0 = parser.arguments();
                        if (args0.size() == 1) {
                            return resolverManager.resolve(args0.get(0), optionType);
                        }
                    }
                    throw new CliError("Missing required option [%s]", option);
                }
            }
            return resolverManager.resolve(argStr, optionType);
        }
    }

    private Object invoke(Object commander, Object[] params) {
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
