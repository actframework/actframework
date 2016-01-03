package testapp.cli;

import act.cli.Command;
import act.cli.HelpMsg;
import act.cli.Optional;
import act.cli.Required;

public class StaticWithoutReturnType {

    private String s;

    @Command("foo.bar")
    @HelpMsg("help")
    public static void doIt(
            @Required(lead = "-o,--op1") String op1,
            @Optional(lead = "-n,--number") int num
    ) {
    }

}
