package playground;

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
import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;

import java.util.Arrays;

public class CBase {

    private static class FakeResult extends Result {
        Object[] args;
        protected FakeResult(Object ... args) {
            super(H.Status.OK);
            this.args = args;
        }

        @Override
        public String toString() {
            return Arrays.toString(args);
        }
    }

    public Result render(Object ... args) {
        return new FakeResult(args);
    }

    public static Result ok() {
        return Ok.get();
    }

    public static Result notFound(String message, Object... args) {
        return new NotFound(message, args);
    }

    public static Result badRequest(String reason, int code, int code2, Integer code3, Integer code4) {
        return new BadRequest(reason);
    }

    public static Result renderStatic(Object ... args) {
        return new FakeResult(args);
    }
}
