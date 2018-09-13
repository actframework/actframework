package ghissues;

import act.controller.Controller;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Map;

@UrlContext("seo")
public class RoutingSEO extends Controller.Util {

    private Map<Integer, String> descLookup = C.Map(1, "/foo", 2, "/bar");

    @GetAction("{id}/...")
    public Integer test(Integer id, String __path) {
        String desc = descLookup.get(id);
        notFoundIfNull(desc);
        redirectIfNot(S.eq(__path, desc), "/seo/" + id + desc);
        return id;
    }

}
