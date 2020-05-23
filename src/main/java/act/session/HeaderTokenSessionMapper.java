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
import act.util.Stateless;
import org.osgl.http.H;
import org.osgl.util.S;

import javax.inject.Inject;

/**
 * Implement {@link SessionMapper} using HTTP header
 */
@Stateless
public class HeaderTokenSessionMapper implements SessionMapper {

    public static final String DEF_HEADER_PREFIX = "X-Act-";
    public static final String DEF_PAYLOAD_PREFIX = "";
    private String sessionHeader;
    private String sessionQueryParamName;
    private String flashHeader;
    private String sessionPayloadPrefix;
    private boolean hasSessionPayloadPrefix;
    private SessionMapper.ExpirationMapper expirationMapper;

    @Inject
    public HeaderTokenSessionMapper(AppConfig config) {
        sessionHeader = config.sessionHeader();
        flashHeader = S.pathConcat(config.sessionHeaderPrefix(), '-', "Flash");
        sessionQueryParamName = config.getSessionQueryParamName();
        sessionPayloadPrefix = config.sessionHeaderPayloadPrefix();
        hasSessionPayloadPrefix = S.notBlank(sessionPayloadPrefix);
        expirationMapper = new ExpirationMapper(config);
    }

    @Override
    public void writeExpiration(long expiration, H.Response response) {
        expirationMapper.writeExpiration(expiration, response);
    }

    @Override
    public void write(String session, String flash, H.Response response) {
        if (null != session && hasSessionPayloadPrefix) {
            session = S.concat(sessionPayloadPrefix, session);
        }
        if (null != session) {
            response.header(sessionHeader, session);
        }
        if (null != flash) {
            response.header(flashHeader, flash);
        }
    }

    @Override
    public String readSession(H.Request request) {
        String payload = request.header(sessionHeader);
        if (null == payload) {
            // try query parameter
            payload = request.paramVal(sessionQueryParamName);
        }
        return null == payload ? null : hasSessionPayloadPrefix ? S.afterFirst(payload, sessionPayloadPrefix) : payload;
    }

    @Override
    public String readFlash(H.Request request) {
        return request.header(flashHeader);
    }
}
