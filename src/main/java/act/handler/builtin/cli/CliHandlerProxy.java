package act.handler.builtin.cli;

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
import act.app.App;
import act.cli.CliContext;
import act.cli.CliException;
import act.cli.CommandExecutor;
import act.cli.bytecode.ReflectedCommandExecutor;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommanderClassMetaInfo;
import act.handler.CliHandlerBase;
import act.util.ProgressGauge;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import java.util.List;

public final class CliHandlerProxy extends CliHandlerBase {

    private static Logger logger = L.get(CliHandlerProxy.class);

    private App app;
    private CommandMethodMetaInfo methodMetaInfo;
    private CommanderClassMetaInfo classMetaInfo;

    private volatile CommandExecutor executor = null;

    public CliHandlerProxy(CommanderClassMetaInfo classMetaInfo, CommandMethodMetaInfo metaInfo, App app) {
        this.methodMetaInfo = $.requireNotNull(metaInfo);
        this.classMetaInfo = $.requireNotNull(classMetaInfo);
        this.app = $.requireNotNull(app);
    }

    @Override
    protected void releaseResources() {
        if (null != executor) {
            executor.destroy();
            executor = null;
        }
    }

    public CommandMethodMetaInfo methodMetaInfo() {
        return methodMetaInfo;
    }

    public CommanderClassMetaInfo classMetaInfo() {
        return classMetaInfo;
    }

    @Override
    public boolean appliedIn(Act.Mode mode) {
        return mode == Act.Mode.DEV || mode == methodMetaInfo.mode();
    }

    @Override
    public void handle(CliContext context) {
        try {
            ensureAgentsReady();
            saveCommandPath(context);
            Object result = _handle(context);
            onResult(result, context);
        } catch ($.Break b) {
            throw b;
        } catch (CliException error) {
            context.println(error.getMessage());
        } catch (Exception e) {
            String msg = e.getMessage();
            if (S.blank(msg)) {
                msg = S.fmt("Error processing command: %s", e);
            }
            context.println(msg);
            logger.error(e, "Error handling request");
        } catch (Throwable t) {
            logger.fatal(t, "Error handling request");
        } finally {
            PropertySpec.current.remove();
        }
    }

    @Override
    public $.T2<String, String> commandLine() {
        return methodMetaInfo.commandLine(classMetaInfo, app.classLoader());
    }

    @Override
    public List<$.T2<String, String>> options() {
        return methodMetaInfo.options(classMetaInfo, app.classLoader());
    }

    public CommandExecutor executor() {
        ensureAgentsReady();
        return executor;
    }

    @SuppressWarnings("unchecked")
    private void onResult(Object result, CliContext context) throws Exception {
        if (null == result) {
            return;
        }
        if (result instanceof ProgressGauge) {
            context.print(methodMetaInfo, (ProgressGauge) result);
        } else {
            PropertySpec.MetaInfo filter = methodMetaInfo.propertySpec();
            methodMetaInfo.view().print(result, filter, context);
        }
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
        context.commandPath(methodMetaInfo.fullName());
    }

    private void generateExecutor() {
        executor = new ReflectedCommandExecutor(methodMetaInfo, app);
    }


    private Object _handle(CliContext context) {
        return executor.execute(context);
    }

    @Override
    public String toString() {
        return methodMetaInfo.fullName();
    }

    @Override
    public int hashCode() {
        return $.hc(methodMetaInfo);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CliHandlerProxy) {
            CliHandlerProxy that = (CliHandlerProxy)obj;
            return $.eq(that.methodMetaInfo, this.methodMetaInfo);
        }
        return false;
    }
}
