package act.cli.meta;

import act.app.App;
import act.asm.Type;
import org.osgl.$;

/**
 * Store {@link OptionAnnoInfoBase option annotation info} that
 * come from a {@link java.lang.reflect.Field field}
 */
public class FieldOptionAnnoInfo extends OptionAnnoInfoBase {
    private String fieldName;
    private Type type;
    private boolean readFileContent;

    public FieldOptionAnnoInfo(String fieldName, Type type, boolean optional) {
        super(optional);
        this.fieldName = fieldName;
        this.type = type;
    }

    public String fieldName() {
        return fieldName;
    }

    public Class fieldType() {
        return $.classForName(type.getClassName(), App.instance().classLoader());
    }

    public void setReadFileContent() {
        readFileContent = true;
    }

    public boolean readFileContent() {
        return readFileContent;
    }

}
