package act.session;

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

import org.osgl.http.H;

/**
 * A `StateCodec` encode server state (Session/Flash) into string
 * and decode previous encoded state string back to state (Session/Flash)
 */
public interface SessionCodec {

    /**
     * Encode a session into a string
     *
     * @return encoded session string
     */
    String encodeSession(H.Session session);

    /**
     * Encode a session into a string using specified expiry in seconds
     * @param session the session to be encoded.
     * @param ttlInSeconds time to live in seconds
     * @return the encoded session
     */
    String encodeSession(H.Session session, int ttlInSeconds);

    /**
     * Encode a flash into a string
     * @param flash
     * @return
     */
    String encodeFlash(H.Flash flash);

    /**
     * Decode a session string into a session.
     *
     * @param encodedSession
     *      the encoded session string
     * @param request
     *      the incoming request - used to provide the current URL path
     */
    H.Session decodeSession(String encodedSession, H.Request request);

    /**
     * Decode a flash string into a flash.
     *
     * @param encodedFlash
     *      the encoded flash string
     */
    H.Flash decodeFlash(String encodedFlash);


}
