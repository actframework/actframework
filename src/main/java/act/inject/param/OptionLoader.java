package act.inject.param;

import act.cli.CliContext;
import act.cli.Optional;
import act.cli.ReadFileContent;
import act.cli.Required;
import act.data.FileResolver;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.util.*;

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
    final boolean readFile;
    private final StringValueResolver resolver;

    OptionLoader(String bindName, Optional optional, StringValueResolver resolver, BeanSpec beanSpec) {
        this.bindName = bindName;
        this.required = false;
        this.parseLeads(optional.lead());
        this.defVal = optional.defVal();
        this.requiredGroup = null;
        this.resolver = resolver;
        this.beanSpec = beanSpec;
        this.readFile = beanSpec.rawType() == String.class && beanSpec.hasAnnotation(ReadFileContent.class);
        CliContext.ParsingContextBuilder.foundOptional();
    }

    OptionLoader(String bindName, Required required, StringValueResolver resolver, BeanSpec beanSpec) {
        this.bindName = bindName;
        this.required = true;
        this.parseLeads(required.lead());
        this.defVal = null;
        String group = required.group();
        this.requiredGroup = S.blank(group) ? bindName : group;
        this.resolver = resolver;
        this.beanSpec = beanSpec;
        this.readFile = beanSpec.rawType() == String.class && beanSpec.hasAnnotation(ReadFileContent.class);
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
            val = resolver.resolve(optVal);
        }
        if (readFile) {
            val = IO.readContentAsString(FileResolver.INSTANCE.resolve(S.string(val)));
        }
        if (null == val && null != cachedBean) {
            val = cachedBean;
        }
        if (null == val) {
            if (!required) {
                val = S.notBlank(defVal) ? resolver.resolve(defVal) : resolver.resolve(null);
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
