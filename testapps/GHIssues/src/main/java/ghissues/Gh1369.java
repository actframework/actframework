package ghissues;

import act.controller.Controller;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.storage.impl.SObject;

public class Gh1369 extends BaseController {

    @GetAction("1369")
    public void form() {
        Controller.Util.renderTemplate("/1369.html");
    }

    @PostAction("1369")
    public String test(SObject file, String name) {
        return name + ": " + (null == file ? "null object" : file.asString());
    }

}
