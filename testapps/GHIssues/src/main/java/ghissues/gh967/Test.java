package ghissues.gh967;

import act.controller.annotation.UrlContext;
import ghissues.BaseController;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("967")
public class Test extends BaseController {

    @GetAction
    public String test() {
        XyzService svc = XyzService.instance();
        return svc.doService();
    }

}
