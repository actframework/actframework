package act.cli.meta;

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

import static act.Destroyable.Util.destroyAll;

import act.app.AppClassLoader;
import act.asm.Type;
import act.cli.view.CliView;
import act.sys.meta.SessionVariableAnnoInfo;
import act.util.LogSupportedDestroyableBase;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

/**
 * Stores all class level information to support generating of
 * {@link act.cli.CommandExecutor command executor}
 */
@ApplicationScoped
public class CommanderClassMetaInfo extends LogSupportedDestroyableBase {

    private static CommanderClassMetaInfo NULL = new CommanderClassMetaInfo() {
        @Override
        protected CommanderClassMetaInfo parent(AppClassLoader classLoader) {
            return this;
        }
    };

    public static final String NAME_SEPARATOR = S.COMMON_SEP;

    private Type type;
    private Type superType;
    private boolean isAbstract = false;
    private List<CommandMethodMetaInfo> commands = new ArrayList<>();
    private Map<String, FieldOptionAnnoInfo> fieldOptionAnnoInfoMap = new HashMap<>();
    private Map<String, SessionVariableAnnoInfo> fieldSessionVariableAnnoInfoMap = new HashMap<>();
    // commandLookup index command method by method name
    private Map<String, CommandMethodMetaInfo> commandLookup = null;
    private CommanderClassMetaInfo parent;
    private String contextPath;
    private CliView view;

    public CommanderClassMetaInfo className(String name) {
        this.type = Type.getObjectType(name);
        return this;
    }

    @Override
    protected void releaseResources() {
        destroyAll(commands, ApplicationScoped.class);
        commands.clear();
        if (null != commandLookup) {
            destroyAll(commandLookup.values(), ApplicationScoped.class);
            commandLookup.clear();
        }
        if (null != parent) parent.destroy();
        super.releaseResources();
    }

    public boolean isCommander() {
        return !commands.isEmpty();
    }

    public String className() {
        return type.getClassName();
    }

    public String internalName() {
        return type.getInternalName();
    }

    public Type type() {
        return type;
    }

    public CommanderClassMetaInfo superType(Type type) {
        superType = type;
        return this;
    }

    public CommanderClassMetaInfo view(CliView view) {
        this.view = view;
        return this;
    }

    public CliView view() {
        return view;
    }

    public CommanderClassMetaInfo addFieldOptionAnnotationInfo(FieldOptionAnnoInfo info) {
        info.paramName(info.fieldName());
        fieldOptionAnnoInfoMap.put(info.fieldName(), info);
        return this;
    }

    public CommanderClassMetaInfo addFieldSessionVariableAnnotInfo(String fieldName, SessionVariableAnnoInfo info) {
        fieldSessionVariableAnnoInfoMap.put(fieldName, info);
        return this;
    }

    public FieldOptionAnnoInfo fieldOptionAnnoInfo(String name) {
        FieldOptionAnnoInfo info = fieldOptionAnnoInfoMap.get(name);
        if (null == info && null != parent) {
            info = parent.fieldOptionAnnoInfo(name);
        }
        return info;
    }

    public SessionVariableAnnoInfo fieldSessionVariableAnnoInfo(String name) {
        SessionVariableAnnoInfo info = fieldSessionVariableAnnoInfoMap.get(name);
        if (null == info && null != parent) {
            info = parent.fieldSessionVariableAnnoInfo(name);
        }
        return info;
    }

    public List<FieldOptionAnnoInfo> fieldOptionAnnoInfoList(AppClassLoader appClassLoader) {
        C.List<FieldOptionAnnoInfo> list = C.list(fieldOptionAnnoInfoMap.values());
        CommanderClassMetaInfo p = parent(appClassLoader);
        if (null != p && NULL != p) {
            list = list.append(p.fieldOptionAnnoInfoList(appClassLoader));
        }
        return list;
    }

    public boolean hasOption(AppClassLoader classLoader) {
        boolean retVal = !fieldOptionAnnoInfoMap.isEmpty();
        if (retVal) {
            return true;
        }
        CommanderClassMetaInfo p = parent(classLoader);
        if (null != p && NULL != p) {
            return p.hasOption(classLoader);
        }
        return false;
    }

    protected CommanderClassMetaInfo parent(AppClassLoader classLoader) {
        if (null == parent) {
            CommanderClassMetaInfoManager manager = classLoader.commanderClassMetaInfoManager();
            parent = manager.commanderMetaInfo(superType.getClassName());
            if (null == parent) {
                parent = NULL;
            }
        }
        return parent;
    }

    public Type superType() {
        return superType;
    }

    public CommanderClassMetaInfo setAbstract() {
        isAbstract = true;
        return this;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public CommanderClassMetaInfo addCommand(CommandMethodMetaInfo info) {
        commands.add(info);
        return this;
    }

    public CommandMethodMetaInfo command(String name) {
        if (null == commandLookup) {
            buildCommandLookup();
        }
        return commandLookup.get(name);
    }

    public boolean hasCommand() {
        return !commands.isEmpty();
    }

    public List<CommandMethodMetaInfo> commandList() {
        return C.list(commands);
    }

    public String contextPath() {
        return contextPath;
    }

    public CommanderClassMetaInfo contextPath(String path) {
        contextPath = path;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CommanderClassMetaInfo) {
            CommanderClassMetaInfo that = $.cast(obj);
            return $.eq(that.type, this.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return $.hc(type);
    }

    private void buildCommandLookup() {
        Map<String, CommandMethodMetaInfo> lookup = new HashMap<>();
        for (CommandMethodMetaInfo command : commands) {
            String[] commandNames = command.commandName().split(NAME_SEPARATOR);
            for (String commandName : commandNames) {
                lookup.put(commandName, command);
            }
            lookup.put(command.methodName(), command);
        }
        commandLookup = lookup;
    }


}
