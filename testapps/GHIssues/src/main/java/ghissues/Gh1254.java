package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

@UrlContext("1254")
public class Gh1254 extends BaseController {

    @GetAction
    public String get() {
        throw new IllegalArgumentException();
    }

}
