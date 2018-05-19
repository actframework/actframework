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
import act.app.ActionContext;
import act.session.JsonWebTokenSessionCodec;
import org.osgl.http.H;
import org.osgl.util.S;

/**
 * Render JSON Web Token
 */
public class RenderJwt extends RenderAny {

    public static final RenderJwt INSTANCE = new RenderJwt();

    public RenderJwt() {
    }

    public void apply(ActionContext context) {
        context.accept(H.Format.JSON);
        JsonWebTokenSessionCodec codec = Act.getInstance(JsonWebTokenSessionCodec.class);
        String token = codec.encodeSession(context.session());
        //H.Request req = context.req();
        H.Response resp = context.resp();
        //applyBeforeCommitHandler(req, resp);
        resp.writeContent(S.concat("{\"token\":\"", token, "\"}"));
        //applyAfterCommitHandler(req, resp);
    }

    public static RenderJwt get() {
        return INSTANCE;
    }
}
