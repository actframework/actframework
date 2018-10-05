package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("api/v1/869")
public class Gh869 extends BaseController {

    @GetAction
    public void test() {}

}
