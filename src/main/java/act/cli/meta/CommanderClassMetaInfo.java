package act.cli.meta;

import act.app.AppClassLoader;
import act.asm.Type;
import act.sys.meta.SessionVariableAnnoInfo;
import act.util.DestroyableBase;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

import static act.Destroyable.Util.destroyAll;

/**
 * Stores all class level information to support generating of
 * {@link act.cli.CommandExecutor command executor}
 */
@ApplicationScoped
public class CommanderClassMetaInfo extends DestroyableBase {

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
    private C.List<CommandMethodMetaInfo> commands = C.newList();
    private C.Map<String, FieldOptionAnnoInfo> fieldOptionAnnoInfoMap = C.newMap();
    private C.Map<String, SessionVariableAnnoInfo> fieldSessionVariableAnnoInfoMap = C.newMap();
    // commandLookup index command method by method name
    private C.Map<String, CommandMethodMetaInfo> commandLookup = null;
    private CommanderClassMetaInfo parent;
    private String contextPath;

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
        if (S.blank(path)) {
            contextPath = "/";
        } else {
            contextPath = path;
        }
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
        C.Map<String, CommandMethodMetaInfo> lookup = C.newMap();
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
