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

import act.Act;
import act.app.SourceInfo;
import act.util.ActError;
import org.osgl.mvc.result.Unauthorized;

import java.util.List;

public class ActUnauthorized extends Unauthorized implements ActError {

    private SourceInfo sourceInfo;

    public ActUnauthorized() {
        super();
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActUnauthorized(String realm) {
        super(realm);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    public ActUnauthorized(String realm, boolean digest) {
        super(realm, digest);
        if (Act.isDev()) {
            loadSourceInfo();
        }
    }

    private void loadSourceInfo() {
        doFillInStackTrace();
        Throwable cause = getCause();
        sourceInfo = Util.loadSourceInfo(null == cause ? getStackTrace() : cause.getStackTrace(), ActUnauthorized.class);
    }

    @Override
    public Throwable getCauseOrThis() {
        return this;
    }

    public SourceInfo sourceInfo() {
        return sourceInfo;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        StackTraceElement[] raw = super.getStackTrace();
        int len = raw.length - 3;
        StackTraceElement[] effective = new StackTraceElement[len];
        System.arraycopy(raw, 3, effective, 0, len);
        return effective;
    }

    public List<String> stackTrace() {
        return Util.stackTraceOf(this);
    }

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        return false;
    }

    public static Unauthorized create() {
        return Act.isDev() ? new ActUnauthorized() : Unauthorized.get();
    }

    public static Unauthorized create(String realm) {
        return Act.isDev() ? new ActUnauthorized(realm) : Unauthorized.of(realm);
    }

    public static Unauthorized create(String realm, boolean digest) {
        return Act.isDev() ? new ActUnauthorized(realm, digest) : new Unauthorized(realm, digest);
    }

}
