package testapp.cli;

import act.cli.Command;
import act.cli.HelpMsg;
import act.cli.Optional;
import act.cli.Required;
import act.util.DataView;

import java.util.List;

public class InstanceWithReturnType {

    private String s;

    @Command("user.list")
    @DataView("fn as firstName,ln as lastName")
    public List<String> getUserList(
            @Required(value = "-i,--id", group = "group1") String id,
            boolean b,
            @Optional(defVal = "-1") int limit,
            long l
    ) {
        return null;
    }

}
