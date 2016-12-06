package act;

import act.util.Banner;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;

import static act.controller.Controller.Util.text;

public class Info {

    @GetAction("/~info")
    public static Result show() {
        return text(Banner.cachedBanner());
    }

}
