package act;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import org.joda.time.DateTime;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;

public class Trace {
    public static final Logger LOGGER_REQUEST = LogManager.get("act.trace.request");
    public static final Logger LOGGER_HANDLER = LogManager.get("act.trace.handler");

    public static class AccessLog {
        private H.Method method;
        private String url;
        private String remoteAddress;
        private String userAgent;
        private String time;

        public void logWithResponseCode(int responseCode) {
            LOGGER_REQUEST.trace("%s [%s] \"%s\" %s %s", remoteAddress, time, method.name(), url, responseCode, userAgent);
        }

        public static AccessLog create(H.Request req) {
            AccessLog log = new AccessLog();
            log.method = req.method();
            log.url = req.url();
            log.remoteAddress = req.ip();
            log.userAgent = req.userAgentStr();
            log.time = DateTime.now().toString();
            return log;
        }


    }
}
