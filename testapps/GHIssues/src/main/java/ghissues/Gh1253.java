package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;

@UrlContext("1253")
public class Gh1253 extends BaseController {

    @LoadResource("1253.mapping")
    private C.Map<String, String> mapping;

    @GetAction
    public C.Map<String, String> get() {
        return mapping;
    }

}
