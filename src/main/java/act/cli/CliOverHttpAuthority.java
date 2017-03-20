package act.cli;

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

/**
 * A simple interface for authorization
 */
public interface CliOverHttpAuthority {
    /**
     * Implementation shall provide the authorization logic in this method to check
     * if the current principal has access to CliOverHttp facilities.
     *
     * If the current principal has no permission, the implementation shall provide
     * relevant logic, e.g. to throw out {@link org.osgl.mvc.result.Forbidden} or
     * throw out a {@link org.osgl.mvc.result.Redirect redirection} to login page etc
     */
    void authorize();

    class AllowAll implements CliOverHttpAuthority {
        @Override
        public void authorize() {
            // just allow it
        }
    }
}
