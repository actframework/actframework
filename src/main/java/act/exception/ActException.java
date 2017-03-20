package act.exception;

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

import org.osgl.exception.UnexpectedException;

import java.util.concurrent.atomic.AtomicLong;

public class ActException extends UnexpectedException {
    private static AtomicLong atomicLong = new AtomicLong(System.currentTimeMillis());
    private String id;

    public ActException() {
        super();
        setId();
    }

    public ActException(String message) {
        super(message);
        setId();
    }

    public ActException(String message, Object... args) {
        super(message, args);
        setId();
    }

    public ActException(Throwable cause) {
        super(cause);
        setId();
    }

    public ActException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
        setId();
    }

    private void setId() {
        long nid = atomicLong.incrementAndGet();
        id = Long.toString(nid, 26);
    }

    public String id() {
        return id;
    }
}
