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

import act.conf.AppConfig;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.http.H;

import java.util.Locale;

/**
 * A `StateMapper` maps serialized state (from session/flash) to
 * {@link org.osgl.http.H.Response response} or from
 * {@link org.osgl.http.H.Request request}
 */
public interface SessionMapper {

    /**
     * Write session expiration date/time into {@link H.Response response}.
     *
     * The expiration will use date time format as specified in
     * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">rfc-7231</a>
     *
     * @param expiration
     *      the expiration in milliseconds
     * @param response
     *      the response to which the expiration to be written
     */
    void writeExpiration(long expiration, H.Response response);

    /**
     * Write serialized session and flash state into {@link H.Response response}.
     *
     * @param session
     *      the session state (a string) to be written to the response
     * @param flash
     *      the flash state (a string) to be written to the response
     * @param response
     *      the HTTP response
     */
    void write(String session, String flash, H.Response response);

    /**
     * Read the incoming HTTP request and extract serialized session state.
     *
     * @param request
     *      the incoming HTTP request
     * @return
     *      session state (a string) read from request or `null` if not
     *      session state found in the request using this mapper
     */
    String readSession(H.Request request);

    /**
     * Read the incoming HTTP request and extract serialized flash state.
     *
     * @param request
     *      the incoming HTTP request
     * @return
     *      flash state (a string) read from request or `null` if not
     *      flash state found in the request using this mapper
     */
    String readFlash(H.Request request);

    class ExpirationMapper {
        private static DateTimeFormatter format = DateTimeFormat.forPattern("E, dd MMM Y HH:mm:ss").withLocale(Locale.ENGLISH);
        boolean enabled;
        String headerName;

        ExpirationMapper (AppConfig conf) {
            enabled = conf.sessionOutputExpiration();
            if (enabled) {
                headerName = conf.headerSessionExpiration();
            }
        }

        public void writeExpiration(long expiration, H.Response response) {
            if (!enabled) {
                return;
            }
            response.header(headerName, expirationInIMF(expiration));
        }

        private static String expirationInIMF(long expirationInMillis) {
            LocalDateTime localDateTime = new LocalDateTime(expirationInMillis, DateTimeZone.UTC);
            return format.print(localDateTime) + " GMT";
        }

    }


}
