package act.inject.param;

import act.cli.CliContext;
import act.cli.Optional;
import act.cli.Required;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

/**
 * Load command line options
 */
class OptionLoader extends CliParamValueLoader implements ParamValueLoader {

    final String bindName;
    String lead1;
    String lead2;
    final String defVal;
    final String requiredGroup;
    final boolean required;
    final BeanSpec beanSpec;
    final String errorTemplate;
    private final StringValueResolver resolver;

    OptionLoader(String bindName, Optional optional, StringValueResolver resolver, BeanSpec beanSpec) {
        this.bindName = bindName;
        this.required = false;
        this.parseLeads(optional.lead());
        this.defVal = optional.defVal();
        this.requiredGroup = null;
        this.beanSpec = beanSpec;
        this.errorTemplate = errorTemplate(optional);
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundOptional();
    }

    OptionLoader(String bindName, Required required, StringValueResolver resolver, BeanSpec beanSpec) {
        this.bindName = bindName;
        this.required = true;
        this.parseLeads(required.lead());
        this.defVal = null;
        String group = required.group();
        this.requiredGroup = S.blank(group) ? bindName : group;
        this.beanSpec = beanSpec;
        this.errorTemplate = errorTemplate(required);
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundRequired(this.requiredGroup);
    }

    @Override
    public Object load(Object cachedBean, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        String optVal = ctx.paramVal(bindName);
        if (S.blank(optVal) && required) {
            optVal = getFirstArgument(ctx);
        }
        Object val = null;
        if (S.notBlank(optVal)) {
            val = resolve(optVal);
        }
        if (null == val && null != cachedBean) {
            val = cachedBean;
        }
        if (null == val) {
            if (!required) {
                val = S.notBlank(defVal) ? resolve(defVal) : resolve(null);
            }
        }
        if (null != val && required) {
            ctx.parsingContext().foundRequired(requiredGroup);
        }
        return val;
    }

    @Override
    public String bindName() {
        return this.bindName;
    }

    private Object resolve(String val) {
        return resolve(val, resolver);
    }

    private <T> T resolve(String val, StringValueResolver<T> resolver) {
        if (null == errorTemplate) {
            return resolver.resolve(val);
        }
        try {
            return resolver.resolve(val);
        } catch (Exception e) {
            throw E.unexpected(errorTemplate, val);
        }
    }

    private String errorTemplate(Optional optional) {
        return verifyErrorTemplate(optional.errorTemplate());
    }

    private String errorTemplate(Required required) {
        return verifyErrorTemplate(required.errorTemplate());
    }

    private String verifyErrorTemplate(String s) {
        if (S.blank(s)) {
            return null;
        }
        if (!s.contains("%")) {
            throw E.invalidConfiguration("Error template must have format argument placeholder, e.g. %s inside it: " + s);
        }
        if (s.split("%").length > 2) {
            throw E.invalidConfiguration("Error template must not have  more than one format argument placeholder, e.g. %s inside it: " + s);
        }
        return s;
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
