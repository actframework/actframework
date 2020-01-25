package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.PostAction;

@UrlContext("1284")
public class Gh1284 extends BaseController {
    @PostAction("{id}")
    public void testPost(String id) {
    }
}
