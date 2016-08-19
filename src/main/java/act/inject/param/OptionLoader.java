package act.inject.param;

import act.app.CliContext;
import act.cli.CliException;
import act.util.ActContext;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

/**
 * Load command line options
 */
class OptionLoader extends CliParamValueLoader implements ParamValueLoader {

    private final String bindName;
    private final String lead1;
    private final String lead2;
    private final String defVal;
    private final String help;
    private final boolean required;
    private final boolean sessionScoped;
    private final StringValueResolver resolver;

    OptionLoader(String bindName, String lead1, String lead2, String defVal, boolean required, String help, boolean sessionScoped, StringValueResolver resolver) {
        this.bindName = bindName;
        this.lead1 = lead1;
        this.lead2 = lead2;
        this.defVal = defVal;
        this.help = help;
        this.required = required;
        this.sessionScoped = sessionScoped;
        this.resolver = resolver;
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        String optVal = optionValue(lead1, lead2, ctx);
        if (S.blank(optVal)) {
            if (!multipleParams(ctx)) {
                optVal = getFirstArgument(ctx);
            }
        }
        Object val = null;
        if (S.notBlank(optVal)) {
            val = resolver.resolve(optVal);
        }
        if (null == val && sessionScoped) {
            val = sessionVal(bindName, ctx);
        }
        if (null == val) {
            if (required) {
                throw new CliException("Missing required option [%s %s]", leads(), help);
            } else {
                val = S.notBlank(defVal) ? resolver.resolve(defVal) : null;
            }
        }
        return val;
    }

    private String leads() {
        if (null == lead1 && null == lead2) {
            return "";
        }
        if (null == lead1) {
            return lead2;
        } else if (null == lead2) {
            return lead1;
        }
        return S.join(",", lead1, lead2);
    }

}
