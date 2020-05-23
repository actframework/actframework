package ghissues;

import act.controller.annotation.UrlContext;
import act.util.PropertySpec;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.N;
import org.osgl.util.S;

import java.io.File;

import static act.controller.Controller.Util.renderTemplate;

@UrlContext("1316")
public class Gh1316 extends BaseController {

    @GetAction
    public void form() {
        renderTemplate("/1316.html");
    }

    @PostAction()
    public String filetest(File[] files1, File[] files2) {
        if (files1.length == 0 && files2.length == 0) {
            return null;
        }
        return "found";
    }

}
