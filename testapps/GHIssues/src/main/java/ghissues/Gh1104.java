package ghissues;

import act.controller.annotation.UrlContext;
import act.util.JsonView;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.N;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.Map;

@UrlContext("1104")
@SuppressWarnings("unused")
public class Gh1104 extends BaseController {

    @GetAction
    public Iterable<String> test(H.Request<?> req) {
        return req.headerNames();
    }

    @GetAction("all")
    public Map<String, Iterable<String>> allHeaders(H.Request<?> req) {
        Map<String, Iterable<String>> map = new HashMap<>();
        for (String name : req.headerNames()) {
            map.put(name, req.headers(name));
        }
        return map;
    }
}
