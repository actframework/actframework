package act.controller.meta;

import act.app.App;
import act.asm.Type;
import act.cli.meta.OptionAnnoInfoBase;
import org.osgl.$;

/**
 * Stores field information when it is annotated with {@link act.di.PathVariable}
 */
public class FieldPathVariableInfo {
    private String fieldName;
    private String pathVariable;
    private Type type;
    private boolean optional;

    public FieldPathVariableInfo(String fieldName, Type type, String pathVariable, boolean optional) {
        this.fieldName = fieldName;
        this.type = type;
        this.pathVariable = pathVariable;
        this.optional = optional;
    }

    public String fieldName() {
        return fieldName;
    }

    public Class fieldType() {
        return $.classForName(type.getClassName(), App.instance().classLoader());
    }

    public String pathVariable() {
        return pathVariable;
    }

    public boolean optional() {
        return optional;
    }

}
