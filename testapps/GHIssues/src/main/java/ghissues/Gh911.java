package ghissues;

import act.controller.annotation.UrlContext;
import org.joda.time.DateTime;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;
import org.rythmengine.utils.Time;

@UrlContext("911")
public class Gh911 extends BaseController {

    @GetAction
    public DateTime getTime(String delta) {
        DateTime now = DateTime.now();
        int seconds;
        if (null != delta) {
            if (S.isInt(delta)) {
                seconds = Integer.parseInt(delta);
            } else {
                boolean negative = false;
                if (delta.startsWith("+")) {
                    delta = delta.substring(1);
                } else if (delta.startsWith("-")) {
                    delta = delta.substring(1);
                    negative = true;
                } else {
                    throw new IllegalArgumentException("Unknown delta: " + delta);
                }
                seconds = Time.parseDuration(delta);
            }
            now = now.plusSeconds(seconds);
        }
        return now;
    }

}
