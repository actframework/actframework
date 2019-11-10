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

import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class CompoundSesssionMapper implements SessionMapper {

    private List<SessionMapper> sessionMappers;

    public CompoundSesssionMapper(SessionMapper ... mappers) {
        this.sessionMappers = C.listOf(mappers);
    }

    public CompoundSesssionMapper(List<SessionMapper> mappers) {
        this.sessionMappers = $.requireNotNull(mappers);
    }

    @Override
    public void writeExpiration(long expiration, H.Response response) {
        for (SessionMapper mapper : sessionMappers) {
            mapper.writeExpiration(expiration, response);
            // all mapper shall use the same expiration output logic
            // so we just break it out here
            break;
        }
    }

    @Override
    public void write(String session, String flash, H.Response response) {
        for (SessionMapper mapper : sessionMappers) {
            mapper.write(session, flash, response);
        }
    }

    @Override
    public String readSession(H.Request request) {
        for (SessionMapper mapper : sessionMappers) {
            String retVal = mapper.readSession(request);
            if (S.notBlank(retVal)) {
                return retVal;
            }
        }
        return null;
    }

    @Override
    public String readFlash(H.Request request) {
        for (SessionMapper mapper : sessionMappers) {
            String retVal = mapper.readFlash(request);
            if (null != retVal) {
                return retVal;
            }
        }
        return null;
    }
}
