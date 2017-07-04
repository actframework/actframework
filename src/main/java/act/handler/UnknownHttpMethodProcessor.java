package act.handler;

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
import org.osgl.mvc.result.MethodNotAllowed;
import org.osgl.mvc.result.Result;

import java.io.Serializable;

public abstract class UnknownHttpMethodProcessor implements Serializable {

    public static UnknownHttpMethodProcessor METHOD_NOT_ALLOWED = new NotAllowed();

    public static UnknownHttpMethodProcessor NOT_IMPLEMENTED = new NotImplemented();

    public abstract Result handle(H.Method method);

    private static class NotAllowed extends UnknownHttpMethodProcessor implements ExpressHandler {
        @Override
        public Result handle(H.Method method) {
            return MethodNotAllowed.get();
        }

        private Object readResolve() {
            return METHOD_NOT_ALLOWED;
        }
    }

    private static class NotImplemented extends UnknownHttpMethodProcessor implements ExpressHandler {
        @Override
        public Result handle(H.Method method) {
            return org.osgl.mvc.result.NotImplemented.INSTANCE;
        }
        private Object readResolve() {
            return NOT_IMPLEMENTED;
        }
    }
}
