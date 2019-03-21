package act.data;

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
import act.cli.CliContext;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Resolver String value into SObject
 */
public class SObjectResolver extends StringValueResolverPlugin<SObject> {

    public static final SObjectResolver INSTANCE = new SObjectResolver();

    private volatile transient OkHttpClient http;

    public SObjectResolver() {
    }

    @Override
    public SObject resolve(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return resolveFromURL(value);
        } else if (value.startsWith("data:")) {
            int pos = value.indexOf("base64,");
            if (pos < 0) {
                return null;
            }
            String encoded = value.substring(pos + "base64,".length());
            return resolveFromBase64(encoded);
        } else {
            File file;
            CliContext cli = CliContext.current();
            if (null != cli) {
                file = cli.getFile(value);
                if (file.exists() && file.canRead()) {
                    return SObject.of(file);
                }
            } else {
                ActionContext act = ActionContext.current();
                if (null != act) {
                    ISObject sobj = act.upload(value);
                    if (null != sobj) {
                        return (SObject)sobj;
                    }
                }
            }
            if (S.blank(value)) {
                return null;
            }
            if (value.length() < 15) {
                // assume it is model name (the parameter name)
                return null;
            }
            // last try base64 decoder
            try {
                return resolveFromBase64(value);
            } catch (Exception e) {
                Act.LOGGER.warn(S.concat("Cannot resolve SObject from value: ", value));
                return null;
            }
        }
    }

    private SObject resolveFromURL(String url) {
        try {
            Response resp = http().newCall(new Request.Builder().url(url).build()).execute();
            return SObject.of(resp.body().byteStream());
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    private SObject resolveFromBase64(String encoded) {
        return SObject.of(Codec.decodeBase64(encoded));
    }

    private OkHttpClient http() {
        if (null == http) {
            synchronized (this) {
                if (null == http) {
                    http = new OkHttpClient.Builder()
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(5, TimeUnit.SECONDS)
                            .writeTimeout(5, TimeUnit.SECONDS).build();
                }
            }
        }
        return http;
    }

}
