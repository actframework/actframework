package act.test.req_modifier;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

import static org.osgl.http.H.Header.Names.*;

import act.test.util.NamedLogic;
import okhttp3.Request;
import org.osgl.util.C;
import org.osgl.util.S;
import org.osgl.util.converter.TypeConverterRegistry;

import java.util.List;

public abstract class RequestModifier extends NamedLogic {

    public abstract void modifyRequest(Request.Builder builder);

    @Override
    protected Class<? extends NamedLogic> type() {
        return RequestModifier.class;
    }

    public static class AcceptJson extends RequestModifier {
        @Override
        public void modifyRequest(Request.Builder builder) {
            builder.header(ACCEPT, "application/json");
        }

        @Override
        protected List<String> aliases() {
            return C.list("need-json", "json");
        }
    }


    public static class RemoteAddress extends RequestModifier {
        @Override
        public void modifyRequest(Request.Builder builder) {
            builder.header(X_FORWARDED_FOR, S.string(initVal));
        }

        @Override
        protected List<String> aliases() {
            return C.list("remote-ip", "ip");
        }
    }

    public static class JsonContent extends RequestModifier {
        @Override
        public void modifyRequest(Request.Builder builder) {
            builder.header(CONTENT_TYPE, "application/json");
        }
    }

    public static void registerTypeConverters() {
        TypeConverterRegistry.INSTANCE.register(new FromLinkedHashMap(RequestModifier.class));
        TypeConverterRegistry.INSTANCE.register(new FromString(RequestModifier.class));
    }

    public static void registerModifiers() {
        new JsonContent().register();
        new AcceptJson().register();
        new RemoteAddress().register();
    }

}
