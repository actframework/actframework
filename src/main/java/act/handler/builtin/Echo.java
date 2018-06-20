package act.handler.builtin;

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

import act.app.ActionContext;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import act.util.ByteBuffers;
import org.osgl.http.H;
import org.osgl.util.S;

import java.nio.ByteBuffer;

public class Echo extends FastRequestHandler implements ExpressHandler {

    private ByteBuffer buffer;
    private String toString;
    private String contentType;

    public Echo(String msg) {
        this(msg, H.Format.TXT.contentType());
    }

    public Echo(String msg, String contentType) {
        this.buffer = ByteBuffers.wrap(msg);
        this.contentType = S.blank(contentType) ? H.Format.TXT.contentType() : contentType;
        this.toString = "echo: " + msg;
    }

    @Override
    public void handle(ActionContext context) {
        H.Response resp = context.resp();
        resp.contentType(contentType);
        resp.writeContent(buffer.duplicate());
    }

    @Override
    public String toString() {
        return toString;
    }

}
