package act.view;

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

import static act.util.ActError.Util.loadSourceInfo;

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import com.alibaba.fastjson.annotation.JSONField;
import org.osgl.mvc.result.Unauthorized;
import org.osgl.util.C;

import java.util.List;

public class ActUnauthorized extends Unauthorized implements ActError {

    public ActUnauthorized() {
        super();
    }

    public ActUnauthorized(int errorCode) {
        super(errorCode);
    }

    public ActUnauthorized(int errorCode, String message) {
        super(errorCode, message);
    }

    public ActUnauthorized(String realm) {
        super(realm);
    }

    public ActUnauthorized(String realm, boolean digest) {
        super(realm, digest);
    }

    @Override
    @JSONField(serialize = false, deserialize = false)
    public Throwable getCauseOrThis() {
        return this;
    }

    public SourceInfo sourceInfo() {
        if (Act.isDev()) {
            return loadSourceInfo(ActUnauthorized.class);
        }
        return null;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return null;
    }

    public List<String> stackTrace() {
        return C.list();
    }

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        return false;
    }

    public static Unauthorized create() {
        return Act.isDev() ? new ActUnauthorized() : Unauthorized.get();
    }

    public static Unauthorized create(int errorCode) {
        return Act.isDev() ? new ActUnauthorized(errorCode) : Unauthorized.of(errorCode);
    }

    public static Unauthorized create(int errorCode, String message) {
        return Act.isDev() ? new ActUnauthorized(errorCode, message) : Unauthorized.of(errorCode, message);
    }

    public static Unauthorized create(String realm) {
        return Act.isDev() ? new ActUnauthorized(realm) : Unauthorized.of(realm);
    }

    public static Unauthorized create(String realm, boolean digest) {
        return Act.isDev() ? new ActUnauthorized(realm, digest) : new Unauthorized(realm, digest);
    }

}
