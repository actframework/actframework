package act.inject.param;

import act.cli.CliContext;
import act.cli.util.CommandLineParser;
import act.util.ActContext;
import org.osgl.inject.util.ArrayLoader;
import org.osgl.util.StringValueResolver;

import java.util.ArrayList;
import java.util.List;

class CliVarArgumentLoader extends CliParamValueLoader {

    private final StringValueResolver resolver;
    private final Class componentType;

    CliVarArgumentLoader(Class componentType, StringValueResolver resolver) {
        this.componentType = componentType;
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundArgument();
    }

    @Override
    public Object load(Object cached, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        List list = new ArrayList();
        CliContext.ParsingContext parsingContext = ctx.parsingContext();
        CommandLineParser parser = ctx.commandLine();
        while (parsingContext.hasArguments(parser)) {
            int id = ctx.parsingContext().curArgId().getAndIncrement();
            list.add(resolver.resolve(parser.argument(id)));
        }
        return ArrayLoader.listToArray(list, componentType);
    }
}
