package gh926;

import act.Act;
import act.cli.Command;
import act.cli.Optional;
import act.inject.DefaultValue;

public class AppEntry {

    @Command("hello")
    public String hello(@Optional @DefaultValue("World") String who) {
        return "Hello " + who;
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }
}
