package gh929;

import act.Act;
import act.cli.Command;
import act.cli.Optional;
import act.cli.Required;
import act.inject.DefaultValue;

public class AppEntry {

    @Command("h1")
    public String style1(@Optional @DefaultValue("World") String who) {
        return "Hello " + who;
    }

    @Command("h2")
    public String style2(@Required @DefaultValue("World") String who) {
        return "Hello " + who;
    }

    @Command("h3")
    public String style3(@DefaultValue("World") String who) {
        return "Hello " + who;
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }
}
