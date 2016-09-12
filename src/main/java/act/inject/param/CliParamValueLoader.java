package act.inject.param;

import act.cli.CliContext;
import act.cli.util.CommandLineParser;

import java.util.List;

abstract class CliParamValueLoader implements ParamValueLoader {

    final String optionValue(String lead1, String lead2, CliContext ctx) {
        return parser(ctx).getString(lead1, lead2);
    }

    final boolean multipleParams(CliContext context) {
        return context.parsingContext().hasMultipleOptionArguments();
    }

    protected final Object sessionVal(String bindName, CliContext context) {
        return context.attribute(bindName);
    }

    protected final String getFirstArgument(CliContext context) {
        List<String> list = arguments(context);
        return list.isEmpty() ? null : list.get(0);
    }

    protected final List<String> arguments(CliContext context) {
        return context.arguments();
    }

    private CommandLineParser parser(CliContext ctx) {
        return ctx.commandLine();
    }

}
