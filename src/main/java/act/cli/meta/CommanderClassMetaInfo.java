package act.cli.meta;

import act.asm.Type;
import act.util.DestroyableBase;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

import static act.Destroyable.Util.destroyAll;

/**
 * Stores all class level information to support generating of
 * {@link act.cli.CommandExecutor command executor}
 */
public final class CommanderClassMetaInfo extends DestroyableBase {

    private Type type;
    private Type superType;
    private boolean isAbstract = false;
    private C.List<CommandMethodMetaInfo> commands = C.newList();
    private C.List<FieldOptionAnnoInfo> fieldOptionAnnoInfoList = C.newList();
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
        destroyAll(commands);
        commands.clear();
        if (null != commandLookup) {
            destroyAll(commandLookup.values());
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
        fieldOptionAnnoInfoList.add(info);
        return this;
    }

    public List<FieldOptionAnnoInfo> fieldOptionAnnoInfoList() {
        C.List<FieldOptionAnnoInfo> list = C.list(fieldOptionAnnoInfoList);
        if (null != parent) {
            list = list.append(parent.fieldOptionAnnoInfoList());
        }
        return list;
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

    public CommanderClassMetaInfo parent(CommanderClassMetaInfo parentInfo) {
        parent = parentInfo;
        return this;
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

    private void buildCommandLookup() {
        C.Map<String, CommandMethodMetaInfo> lookup = C.newMap();
        for (CommandMethodMetaInfo command : commands) {
            lookup.put(command.commandName(), command);
        }
        commandLookup = lookup;
    }

}
