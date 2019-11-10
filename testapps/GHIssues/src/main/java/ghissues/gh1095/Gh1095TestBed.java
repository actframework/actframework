package ghissues.gh1095;

import act.cli.Command;
import act.controller.annotation.UrlContext;
import act.util.JsonView;
import act.util.PropertySpec;
import ghissues.BaseController;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1095")
public class Gh1095TestBed extends BaseController {

    @Command("1095.test")
    @JsonView
    @PropertySpec("name,children.*")
    @GetAction
    public GH1095Department test() {
        GH1095Department c1 = new GH1095Department("c1");
        GH1095Department c2 = new GH1095Department("c2");
        GH1095Department p = new GH1095Department("p");
        p.children.add(c1);
        p.children.add(c2);
        return p;
    }

}
