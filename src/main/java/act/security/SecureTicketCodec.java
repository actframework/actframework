package act.security;

import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encode/decode a secure ticket from/to {@link H.Session}, or part of the session info
 * @param <T> the encoded type
 */
public interface SecureTicketCodec<T> {

    /**
     * Generate a secure ticket from a session data
     * @param session the session data
     * @return a secure ticket
     */
    T createTicket(H.Session session);

    /**
     * Parse a secure ticket and construct a session data
     * @param ticket the secure ticket
     * @return a session data from the ticket
     */
    H.Session parseTicket(T ticket);

    abstract class Base<T> implements SecureTicketCodec<T> {

        protected abstract T serialize(String id, Map<String, String> payload);
        protected abstract Map<String, String> deserialize(T ticket, $.Var<String> id);

        private Set<String> keys;

        public Base() {this(C.<String>set());}

        public Base(Collection<String> keys) {
            this.keys = C.set($.notNull(keys));
        }

        public Base(String ... keys) {
            this(C.listOf(keys));
        }

        public Base(String keys) {
            this(C.listOf(keys.split(S.COMMON_SEP)));
        }

        @Override
        public final T createTicket(H.Session session) {
            String id = session.id();
            Map<String, String> map = new HashMap<>();
            Set<String> keys = this.keys;
            if (keys.isEmpty()) {
                keys = session.keySet();
            }
            for (String key : keys) {
                String val = session.get(key);
                if (null != val) {
                    map.put(key, val);
                }
            }
            return serialize(id, map);
        }

        @Override
        public final H.Session parseTicket(T ticket) {
            $.Var<String> id = $.var();
            Map<String, String> data = deserialize(ticket, id);
            H.Session session = new H.Session();
            $.setField("id", session, id.get());
            if (data.isEmpty()) {
                return session;
            }
            for (Map.Entry<String, String> entry : data.entrySet()) {
                session.put(entry.getKey(), entry.getValue());
            }
            return session;
        }
    }

}
