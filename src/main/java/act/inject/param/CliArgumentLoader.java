package act.inject.param;

import act.cli.CliContext;
import act.util.ActContext;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

class CliArgumentLoader extends CliParamValueLoader {

    private final StringValueResolver resolver;

    CliArgumentLoader(StringValueResolver resolver) {
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundArgument();
    }

    @Override
    public Object load(Object cached, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        int id = ctx.parsingContext().curArgId().getAndIncrement();
        String optVal = ctx.commandLine().argument(id);
        Object val = null;
        if (S.notBlank(optVal)) {
            val = resolver.resolve(optVal);
        }
        if (null == val && null != cached) {
            val = cached;
        }
        return val;
    }

    @Override
    public String bindName() {
        return null;
    }
}
