package ghissues;

import act.controller.annotation.UrlContext;
import org.joda.time.DateTime;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("908")
public class Gh908 extends BaseController {
    @GetAction
    public DateTime test() {
        return DateTime.now().withMillisOfSecond(0);
    }
}
