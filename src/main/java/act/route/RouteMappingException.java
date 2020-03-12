package act.route;

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

import act.app.SourceInfoAvailableActAppException;
import org.osgl.$;
import org.osgl.util.S;

public class RouteMappingException extends SourceInfoAvailableActAppException {

    private String cause;

    public RouteMappingException(String cause, Object ... args) {
        this.cause = $.fmt(cause, args);
    }

    @Override
    public String getErrorTitle() {
        return "Error route mapping";
    }

    @Override
    public String getMessage() {
        return getErrorDescription();
    }

    @Override
    public String getErrorDescription() {
        return S.fmt("Error setting route mapping: %s", cause);
    }

}
