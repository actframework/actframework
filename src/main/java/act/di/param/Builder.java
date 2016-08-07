package act.di.param;

import act.util.ActContext;
import org.osgl.$;

/**
 * Builder is a {@link ParamValueLoader} that build the
 * object instance required from the data provided by
 * the context
 */
class Builder implements ParamValueLoader {
    /**
     * `namePath` is something like `foo.bar.id` etc
     */
    private String[] namePath;
    private Class type;

    Builder(Class type, String[] parentPath, String name) {
        this.namePath = $.concat(parentPath, new String[]{name});
        this.type = type;
    }

    @Override
    public Object load(ActContext context) {
        return null;
    }
}
