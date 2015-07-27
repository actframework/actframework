package act.util;

import act.app.ActionContext;
import org.osgl.http.H;

public interface SessionMapper {

    void serializeSession(H.Cookie sessionCookie, ActionContext context);

    void serializeFlash(H.Cookie flashCookie, ActionContext context);

    String deserializeSession(ActionContext context);

    String deserializeFlash(ActionContext context);

    public static class DefaultSessionMapper implements SessionMapper {
        @Override
        public void serializeSession(H.Cookie sessionCookie, ActionContext context) {
            context.resp().addCookie(sessionCookie);
        }

        @Override
        public void serializeFlash(H.Cookie flashCookie, ActionContext context) {
            context.resp().addCookie(flashCookie);
        }

        @Override
        public String deserializeSession(ActionContext context) {
            H.Cookie sessionCookie = context.req().cookie(context.config().sessionCookieName());
            return null == sessionCookie ? null : sessionCookie.value();
        }

        @Override
        public String deserializeFlash(ActionContext context) {
            H.Cookie flashCookie = context.req().cookie(context.config().flashCookieName());
            return null == flashCookie ? null : flashCookie.value();
        }
    }
}
