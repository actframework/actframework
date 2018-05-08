package act.ws;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
     * Parse a secure ticket and construct a session data. Note
     * if the `ticket` specified is invalid then it shall return
     * a `null` session
     *
     * @param ticket the secure ticket
     * @return a session data from the ticket or `null` if the ticket is invalid to this codec
     */
    H.Session parseTicket(T ticket);

    /**
     * Do sanity check on an object to quickly probe if it is a
     * ticket that can be processed by this codec
     *
     * @param ticket the object to be tested
     * @return `true` if the codec believe it can process the ticket or `false` if not sure
     */
    boolean probeTicket(Object ticket);

    abstract class Base<T> implements SecureTicketCodec<T> {

        /**
         * Encode the session id and payload data into the ticket with type `<T>`
         * @param id the session id
         * @param payload the payload data
         * @return the ticket
         */
        protected abstract T serialize(String id, Map<String, String> payload);

        /**
         * Decode the ticket and return the session ID and fill the payload map
         *
         * Note if the ticket is invalid, the implementation shall return a `null`
         * `id` and leave the `payload` map untouched
         *
         * @param ticket the ticket to be decoded
         * @param payload a Map passed in to be filled with decoded payload
         * @return the session ID decoded from the ticket specified
         */
        protected abstract String deserialize(T ticket, Map<String, String> payload);

        private Set<String> keys;

        public Base() {this(C.<String>set());}

        public Base(Collection<String> keys) {
            this.keys = C.set($.requireNotNull(keys));
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
                keys = C.newSet(session.keySet());
                keys.remove(H.Session.KEY_EXPIRATION);
                keys.remove(H.Session.KEY_ID);
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
            Map<String, String> payload = new HashMap<>();
            String id = deserialize(ticket, payload);
            if (null == payload) {
                return null;
            }
            H.Session session = new H.Session();
            $.setField("id", session, id);
            if (payload.isEmpty()) {
                return session;
            }
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                session.put(entry.getKey(), entry.getValue());
            }
            return session;
        }
    }

}
