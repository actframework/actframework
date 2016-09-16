package act.util;

import act.app.ActionContext;
import org.osgl.http.H;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Map the {@link org.osgl.http.H.Session} and {@link org.osgl.http.H.Cookie} to/from
 * {@link org.osgl.http.H.Response}/{@link org.osgl.http.H.Request}
 */
public interface SessionMapper {

    void serializeSession(H.Cookie sessionCookie, ActionContext context);

    void serializeFlash(H.Cookie flashCookie, ActionContext context);

    String deserializeSession(ActionContext context);

    String deserializeFlash(ActionContext context);

    /**
     * The default session mapper, do the mapping by adding/reading cookie information
     * directly to response/request
     */
    class DefaultSessionMapper implements SessionMapper {

        public static SessionMapper INSTANCE = new DefaultSessionMapper();

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

        public static  SessionMapper wrap(final SessionMapper theMapper) {
            if (null == theMapper) {
                return INSTANCE;
            }
            if (DefaultSessionMapper.class.equals(theMapper.getClass())) {
                return theMapper;
            }
            return new SessionMapper() {
                @Override
                public void serializeSession(H.Cookie sessionCookie, ActionContext context) {
                    theMapper.serializeSession(sessionCookie, context);
                    INSTANCE.serializeSession(sessionCookie, context);
                }

                @Override
                public void serializeFlash(H.Cookie flashCookie, ActionContext context) {
                    theMapper.serializeFlash(flashCookie, context);
                    INSTANCE.serializeFlash(flashCookie, context);
                }

                @Override
                public String deserializeSession(ActionContext context) {
                    String s = theMapper.deserializeSession(context);
                    return S.blank(s) ? INSTANCE.deserializeSession(context) : s;
                }

                @Override
                public String deserializeFlash(ActionContext context) {
                    String s = theMapper.deserializeFlash(context);
                    return S.blank(s) ? INSTANCE.deserializeFlash(context) : s;
                }
            };
        }
    }

    /**
     * The header session mapper do mapping through write/read the header of response/request
     */
    class HeaderSessionMapper implements SessionMapper {

        public static final String DEF_HEADER_PREFIX = "X-Act-";

        private String headerPrefix;

        public HeaderSessionMapper(String headerPrefix) {
            E.illegalArgumentIf(S.blank(headerPrefix));
            this.headerPrefix = headerPrefix;
        }

        public HeaderSessionMapper() {
            this(DEF_HEADER_PREFIX);
        }

        @Override
        public void serializeSession(H.Cookie sessionCookie, ActionContext context) {
            context.resp().header(sessionHeaderName(), sessionCookie.value());
        }

        @Override
        public void serializeFlash(H.Cookie flashCookie, ActionContext context) {
            context.resp().header(flashHeaderName(), flashCookie.value());
        }

        @Override
        public String deserializeSession(ActionContext context) {
            return context.req().header(sessionHeaderName());
        }

        @Override
        public String deserializeFlash(ActionContext context) {
            return context.req().header(flashHeaderName());
        }

        private String sessionHeaderName() {
            return headerPrefix + "Session";
        }

        private String flashHeaderName() {
            return headerPrefix + "Flash";
        }
    }

}
