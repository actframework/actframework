package ghissues.gh1093;

import act.cli.Command;
import act.controller.annotation.UrlContext;
import ghissues.BaseController;
import org.osgl.mvc.annotation.GetAction;

import java.util.ArrayList;
import java.util.List;

@UrlContext("1093")
public class Gh1093Service extends BaseController {

    @GetAction("users")
    @Command("1092.users")
    public List<Gh1093User> listUsers() {
        List<Gh1093User> list = new ArrayList<>();
        Gh1093Department dep = new Gh1093Department();
        dep.name = "RD";
        Gh1093User green = new Gh1093User();
        list.add(green);
        green.name = "green";
        green.department = dep;
        dep.users.add(green);
        return list;
    }

}
