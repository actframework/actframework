package act.cli.meta;

import act.asm.Type;
import act.util.DestroyableBase;
import org.osgl.util.C;
import org.osgl.util.S;

import static act.Destroyable.Util.destroyAll;

/**
 * Stores all class level information to support generating of
 * {@link act.cli.CommandExecutor command executor}
 */
public final class CommanderClassMetaInfo extends DestroyableBase {

    private Type type;
    private Type superType;
    private boolean isAbstract = false;
    private String ctxField = null;
    private boolean ctxFieldIsPrivate = true;
    private C.List<CommandMethodMetaInfo> commands = C.newList();
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

    public CommanderClassMetaInfo ctxField(String fieldName, boolean isPrivate) {
        ctxField = fieldName;
        ctxFieldIsPrivate = isPrivate;
        return this;
    }

    public String nonPrivateCtxField() {
        if (null != ctxField) {
            return ctxFieldIsPrivate ? null : ctxField;
        }
        return null == parent ? null : parent.nonPrivateCtxField();
    }

    public String ctxField() {
        if (null != ctxField) {
            return ctxField;
        }
        if (null != parent) {
            return parent.nonPrivateCtxField();
        }
        return null;
    }

    public boolean hasCtxField() {
        return null != ctxField;
    }

    public boolean ctxFieldIsPrivate() {
        return ctxFieldIsPrivate;
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
