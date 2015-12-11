package act.cli.meta;

import act.asm.Type;
import act.cli.Optional;
import act.cli.Required;
import act.util.DestroyableBase;
import org.osgl.$;

/**
 * Stores Command parameter meta info
 */
public class CommandParamMetaInfo extends DestroyableBase {
    private String name;
    private Type type;
    private Type componentType;
    private OptionAnnoInfo optionInfo;

    public CommandParamMetaInfo name(String name) {
        this.name = $.NPE(name);
        return this;
    }

    public String name() {
        return name;
    }

    public CommandParamMetaInfo type(Type type) {
        this.type = $.NPE(type);
        return this;
    }

    public Type type() {
        return type;
    }

    public CommandParamMetaInfo componentType(Type type) {
        this.componentType = $.NPE(type);
        return this;
    }

    public Type componentType() {
        return componentType;
    }

    public CommandParamMetaInfo optionInfo(OptionAnnoInfo optionInfo) {
        this.optionInfo = $.NPE(optionInfo);
        optionInfo.setLeadsIfNotSet(name);
        return this;
    }

    public OptionAnnoInfo optionInfo() {
        return optionInfo;
    }

}
