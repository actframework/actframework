package act.handler.builtin.cli;

import act.app.App;
import act.cli.CliContext;
import act.cli.CommandExecutor;
import act.controller.meta.ActionMethodMetaInfo;
import act.controller.meta.ControllerClassMetaInfo;
import act.handler.CliHandlerBase;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;

public final class CliHandlerProxy extends CliHandlerBase {

    private static Logger logger = L.get(CliHandlerProxy.class);

    private App app;
    private String commandClassName;
    private String commandMethodName;
    private Boolean requireContextLocal = null;

    private volatile CommandExecutor executor = null;

    public CliHandlerProxy(String commandMethodName, App app) {
        int pos = commandMethodName.lastIndexOf('.');
        final String ERR = "Invalid command method: %s";
        E.illegalArgumentIf(pos < 0, ERR, commandMethodName);
        commandClassName = commandMethodName.substring(0, pos);
        E.illegalArgumentIf(S.isEmpty(commandClassName), ERR, commandMethodName);
        this.commandMethodName = commandMethodName.substring(pos + 1);
        E.illegalArgumentIf(S.isEmpty(this.commandMethodName), ERR, commandMethodName);
        this.app = app;
    }

    @Override
    protected void releaseResources() {
        if (null != executor) {
            executor.destroy();
            executor = null;
        }
    }

    public String commander() {
        return commandClassName;
    }

    public String command() {
        return commandMethodName;
    }

    @Override
    public void handle(CliContext context) {
        try {
            ensureAgentsReady();
            saveCommandPath(context);
            Object result = _handle(context);
            onResult(result, context);
        } catch (Exception e) {
            logger.error(e, "Error handling request");
        }
    }

    private void onResult(Object result, CliContext context) {
        E.tbd();
    }

    private void ensureAgentsReady() {
        if (null == executor) {
            synchronized (this) {
                if (null == executor) {
                    generateExecutor();
                }
            }
        }
    }

    // could be used by View to resolve default path to template
    private void saveCommandPath(CliContext context) {
        StringBuilder sb = S.builder(commandClassName).append(".").append(commandMethodName);
        String path = sb.toString();
        context.commandPath(path);
    }

    private void generateExecutor() {
    }


    private Object _handle(CliContext context) {
        return executor.execute(context);
    }

    private ActionMethodMetaInfo lookupAction() {
        ControllerClassMetaInfo ctrl = app.classLoader().controllerClassMetaInfo(commandClassName);
        return ctrl.action(commandMethodName);
    }

    @Override
    public String toString() {
        return S.fmt("%s.%s", commandClassName, commandMethodName);
    }

}
