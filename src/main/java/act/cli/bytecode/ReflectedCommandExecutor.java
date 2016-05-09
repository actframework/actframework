package act.cli.bytecode;

import act.app.App;
import act.app.CliContext;
import act.app.data.StringValueResolverManager;
import act.cli.CliError;
import act.cli.CommandExecutor;
import act.cli.meta.*;
import act.cli.util.CommandLineParser;
import act.sys.meta.SessionVariableAnnoInfo;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
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
        List<FieldOptionAnnoInfo> list = classMetaInfo.fieldOptionAnnoInfoList(app.classLoader());
        Object cmd = commanderInstance(list, context);
        Object[] params = params(list.size(), context);
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

    private Object commanderInstance(List<FieldOptionAnnoInfo> list, CliContext context) {
        String commander = commanderClass.getName();
        Object inst = context.__commanderInstance(commander);
        if (null == inst) {
            inst = context.newInstance(commanderClass);
            context.__commanderInstance(commander, inst);
        }
        $.Var<Integer> argIdx = $.var(0);
        List<FieldOptionAnnoInfo> unresolved = C.newList();
        for (FieldOptionAnnoInfo fieldOptionAnnoInfo : list) {
            String fieldName = fieldOptionAnnoInfo.fieldName();
            Object sessionVal = null;
            SessionVariableAnnoInfo sessionAttributeAnnoInfo = classMetaInfo.fieldSessionVariableAnnoInfo(fieldName);
            if (null != sessionAttributeAnnoInfo) {
                String key = sessionAttributeAnnoInfo.name();
                sessionVal = context.attribute(key);
            }
            if (null == sessionVal) {
                sessionVal = context.attribute(fieldName);
            }
            if (null == sessionVal) {
                unresolved.add(fieldOptionAnnoInfo);
            } else {
                Object val = optionVal(fieldOptionAnnoInfo.fieldType(), fieldOptionAnnoInfo, argIdx, false, fieldOptionAnnoInfo.readFileContent(), sessionVal, context);
                $.setProperty(inst, val, fieldName);
            }
        }
        boolean one = unresolved.size() == 1 && methodMetaInfo.paramCount() == methodMetaInfo.ctxParamCount();
        for (FieldOptionAnnoInfo fieldOptionAnnoInfo : unresolved) {
            String fieldName = fieldOptionAnnoInfo.fieldName();
            Object val = optionVal(fieldOptionAnnoInfo.fieldType(), fieldOptionAnnoInfo, argIdx, one, fieldOptionAnnoInfo.readFileContent(), null, context);
            $.setProperty(inst, val, fieldName);
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

    private Object[] params(int fieldOptionCount, CliContext ctx) {
        int paramCount = methodMetaInfo.paramCount();
        int ctxParamCount = methodMetaInfo.ctxParamCount();
        Object[] oa = new Object[paramCount];
        if (0 == paramCount) {
            return oa;
        }
        $.Var<Integer> argIdx = $.var(0);
        for (int i = 0; i < paramCount; ++i) {
            CommandParamMetaInfo param = methodMetaInfo.param(i);
            Class<?> paramType = paramTypes[i];
            if (param.isContext()) {
                oa[i] = app.newInstance(paramType);
            } else {
                Object sessionVal = null;
                String sessionVarName = param.cliSessionAttributeKey();
                if (null != sessionVarName) {
                    sessionVal = ctx.attribute(sessionVarName);
                }
                if (null == sessionVal) {
                    sessionVal = ctx.attribute(param.name());
                }
                oa[i] = optionVal(paramType, param.optionInfo(), argIdx, (paramCount - ctxParamCount - fieldOptionCount) == 1, param.readFileContent(), sessionVal, ctx);
            }
        }
        return oa;
    }

    private Object optionVal(Class<?> optionType, OptionAnnoInfoBase option, $.Var<Integer> argIdx,
                             boolean useArgumentIfOptionNotFound, boolean readFileContent, Object cliSessionAttributeVal, CliContext ctx) {
        StringValueResolverManager resolverManager = app.resolverManager();
        CommandLineParser parser = ctx.commandLine();
        List<String> args = ctx.arguments();
        String argStr;
        if (null == option) {
            int i = argIdx.get();
            argStr = args.get(i);
            argIdx.set(i + 1);
        } else {
            argStr = parser.getString(option.lead1(), option.lead2());
            if (S.blank(argStr)) {
                if (useArgumentIfOptionNotFound) {
                    // try to use the single param as the option
                    List<String> args0 = parser.arguments();
                    if (args0.size() == 1) {
                        return resolverManager.resolve(args0.get(0), optionType);
                    }
                }
                if (null != cliSessionAttributeVal) {
                    return cliSessionAttributeVal;
                }
                if (option.required()) {
                    throw new CliError("Missing required option [%s]", option);
                }
            }
        }
        if (File.class.isAssignableFrom(optionType)) {
            if (argStr.startsWith(File.separator) || argStr.startsWith("/")) {
                return new File(argStr);
            } else {
                return new File(ctx.curDir(), argStr);
            }
        } else if (readFileContent) {
            File file;
            if (argStr.startsWith(File.separator) || argStr.startsWith("/")) {
                file = new File(argStr).getAbsoluteFile();
            } else {
                file = new File(ctx.curDir(), argStr);
            }
            if (file.exists()) {
                if (List.class.isAssignableFrom(optionType)) {
                    return IO.readLines(file);
                } else {
                    argStr = IO.readContentAsString(file);
                }
            }
        }
        return resolverManager.resolve(argStr, optionType);
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
