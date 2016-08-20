package act.inject.param;

import act.app.CliContext;
import act.cli.Optional;
import act.cli.Required;
import act.util.ActContext;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

/**
 * Load command line options
 */
class OptionLoader extends CliParamValueLoader implements ParamValueLoader {

    private final String bindName;
    private String lead1;
    private String lead2;
    private final String defVal;
    private final String requiredGroup;
    private final boolean required;
    private final StringValueResolver resolver;

    OptionLoader(String bindName, Optional optional, StringValueResolver resolver) {
        this.bindName = bindName;
        this.required = false;
        this.parseLeads(optional.lead());
        this.defVal = optional.defVal();
        this.requiredGroup = null;
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundOptional();
    }

    OptionLoader(String bindName, Required required, StringValueResolver resolver) {
        this.bindName = bindName;
        this.required = true;
        this.parseLeads(required.lead());
        this.defVal = null;
        String group = required.group();
        this.requiredGroup = S.blank(group) ? bindName : group;
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundRequired(this.requiredGroup);
    }

    @Override
    public Object load(Object cachedBean, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        String optVal = optionValue(lead1, lead2, ctx);
        if (S.blank(optVal)) {
            if (!multipleParams(ctx)) {
                optVal = getFirstArgument(ctx);
            }
        }
        if (S.blank(optVal) && !multipleParams(ctx)) {
            optVal = getFirstArgument(ctx);
        }
        Object val = null;
        if (S.notBlank(optVal)) {
            val = resolver.resolve(optVal);
        }
        if (null == val && null != cachedBean) {
            val = cachedBean;
        }
        if (null == val) {
            if (!required) {
                val = S.notBlank(defVal) ? resolver.resolve(defVal) : null;
            }
        }
        if (null != val && required) {
            ctx.parsingContext().foundRequired(requiredGroup);
        }
        return val;
    }

    private void parseLeads(String[] specs) {
        lead1 = specs[0];
        if (specs.length > 1) {
            lead2 = specs[1];
        } else {
            String[] sa = lead1.split("[,;\\s]+");
            if (sa.length > 2) {
                throw E.unexpected("Option cannot have more than two leads");
            }
            if (sa.length > 1) {
                lead1 = sa[0];
                lead2 = sa[1];
            }
        }
        if (S.blank(lead1)) {
            lead1 = "-" + bindName.charAt(0);
            lead2 = "--" + Keyword.of(bindName).dashed();
        }
    }


}
