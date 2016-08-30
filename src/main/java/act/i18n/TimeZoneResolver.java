package act.i18n;

import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.S;

import java.util.Calendar;

@SuppressWarnings("unused")
public class TimeZoneResolver extends Controller.Util {

    public static final String SESSION_KEY = "__tz__";

    @PostAction("~/i18n/timezone")
    public static void updateTimezoneOffset(int offset, H.Session session) {
        session.put(SESSION_KEY, offset);
    }

    public static int timezoneOffset() {
        return timezoneOffset(H.Session.current());
    }

    public static int timezoneOffset(H.Session session) {
        String s = null != session ? session.get(SESSION_KEY) : null;
        return S.notBlank(s) ? Integer.parseInt(s) : serverTimezoneOffset();
    }

    public static int serverTimezoneOffset() {
        Calendar cal = Calendar.getInstance();
        return -(cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (1000 * 60);
    }

}
