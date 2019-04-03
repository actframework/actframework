package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.N;
import org.osgl.util.S;

@UrlContext("1104")
@JsonView
public class Gh1104 extends BaseController {

    @GetAction
    public Iterable<String> test(H.Request<?> req) {
        return req.headerNames();
    }
}
