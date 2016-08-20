package act.cli.bytecode;

import act.app.App;
import act.app.CliContext;
import act.cli.CommandExecutor;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommandParamMetaInfo;
import act.cli.meta.CommanderClassMetaInfo;
import act.inject.param.CliContextParamLoader;
import act.inject.param.ParamValueLoaderManager;
import act.inject.param.ParamValueLoaderService;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.osgl.$;
import org.osgl.util.E;

import java.lang.reflect.Method;

/**
 * Implement {@link act.cli.CommandExecutor} using
 * https://github.com/EsotericSoftware/reflectasm
 */
public class ReflectedCommandExecutor extends CommandExecutor {

    private static final Object[] DUMP_PARAMS = new Object[0];

    private CommandMethodMetaInfo methodMetaInfo;
    private App app;
    private ParamValueLoaderService paramLoaderService;
    private ClassLoader cl;
    private Class[] paramTypes;
    private Class<?> commanderClass;
    private Method method;
    private MethodAccess methodAccess;
    private int commandIndex;
    private int paramCount;
    private CliContext.ParsingContext parsingContext;

    public ReflectedCommandExecutor(CommandMethodMetaInfo methodMetaInfo, App app) {
        this.methodMetaInfo = $.notNull(methodMetaInfo);
        this.app = $.NPE(app);
        this.cl = app.classLoader();
        this.paramTypes = paramTypes();
        this.paramCount = methodMetaInfo.paramCount();
        this.commanderClass = $.classForName(methodMetaInfo.classInfo().className(), cl);
        try {
            this.method = commanderClass.getMethod(methodMetaInfo.methodName(), paramTypes);
        } catch (NoSuchMethodException e) {
            throw E.unexpected(e);
        }
        if (!methodMetaInfo.isStatic()) {
            methodAccess = MethodAccess.get(commanderClass);
            commandIndex = methodAccess.getIndex(methodMetaInfo.methodName(), paramTypes);
        } else {
            method.setAccessible(true);
        }
        this.paramLoaderService = app.service(ParamValueLoaderManager.class).get(CliContext.class);
        this.buildParsingContext();
    }

    @Override
    public Object execute(CliContext context) {
        //List<FieldOptionAnnoInfo> list = classMetaInfo.fieldOptionAnnoInfoList(app.classLoader());
        //Object cmd = commanderInstance(list, context);
        //Object[] params = params(context);
        context.prepare(parsingContext);
        Object cmd = commanderInstance(context);
        Object[] params = params2(context);
        context.parsingContext().raiseExceptionIfThereAreMissingOptions();
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
            inst = paramLoaderService.loadHostBean(commanderClass, context);
            context.__commanderInstance(commander, inst);
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

    private Object[] params2(CliContext ctx) {
        if (0 == paramCount) {
            return DUMP_PARAMS;
        }
        return paramLoaderService.loadMethodParams(method, ctx);
    }

    private Object invoke(Object commander, Object[] params) {
        Object result;
        if (null != methodAccess) {
            try {
                result = methodAccess.invoke(commander, commandIndex, params);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        } else {
            try {
                result = method.invoke(null, params);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw E.unexpected(e);
            }
        }
        return result;
    }

    private void buildParsingContext() {
        CliContextParamLoader loader = (CliContextParamLoader) app.service(ParamValueLoaderManager.class).get(CliContext.class);
        this.parsingContext = loader.buildParsingContext(commanderClass, method, methodMetaInfo);
    }

}
