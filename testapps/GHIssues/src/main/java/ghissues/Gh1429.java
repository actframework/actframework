package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import act.util.PropertySpec;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.N;
import org.osgl.util.S;

@UrlContext("1429")
public class Gh1429 extends BaseController {

    @GetAction
    public String echo(String message) {
        return message;
    }
}
