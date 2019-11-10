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

import act.app.ActionContext;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.mvc.result.ErrorResult;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.Codec;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Disclaim the source code is copied from Play!Framework 1.3
public class UrlEncodedParser extends RequestBodyParser {
    
    boolean forQueryString = false;

    @Override
    public Map<String, String[]> parse(ActionContext context) {
        H.Request request = context.req();
        // Encoding is either retrieved from contentType or it is the default encoding
        final String encoding = request.characterEncoding();
        InputStream is = request.inputStream();
        try {
            Map<String, String[]> params = new LinkedHashMap<String, String[]>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) > 0) {
                os.write(buffer, 0, bytesRead);
            }

            String data = new String(os.toByteArray(), encoding);
            if (data.length() == 0) {
                //data is empty - can skip the rest
                return new HashMap<>(0);
            }

            // check if data is in JSON format
            if (data.startsWith("{") && data.endsWith("}") || data.startsWith("[") && data.endsWith("]")) {
                return C.Map(ActionContext.REQ_BODY, new String[]{data});
            }

            // data is o the form:
            // a=b&b=c%12...

            // Let us lookup in two phases - we wait until everything is parsed before
            // we decoded it - this makes it possible for use to look for the
            // special _charset_ param which can hold the charset the form is encoded in.
            //
            // http://www.crazysquirrel.com/computing/general/form-encoding.jspx
            // https://bugzilla.mozilla.org/show_bug.cgi?id=18643
            //
            // NB: _charset_ must always be used with accept-charset and it must have the same value

            String[] keyValues = data.split("&");

            int httpMaxParams = context.app().config().httpMaxParams();
            // to prevent the server from being vulnerable to POST hash collision DOS-attack (Denial of Service through hash table multi-collisions),
            // we should by default not lookup the params into HashMap if the count exceeds a maximum limit
            if (httpMaxParams != 0 && keyValues.length > httpMaxParams) {
                logger.warn("Number of request parameters %d is higher than maximum of %d, aborting. Can be configured using 'act.http.params.max'", keyValues.length, httpMaxParams);
                throw new ErrorResult(H.Status.valueOf(413)); //413 Request Entity Too Large
            }

            for (String keyValue : keyValues) {
                // split this key-value on the first '='
                int i = keyValue.indexOf('=');
                String key = null;
                String value = null;
                if (i > 0) {
                    key = keyValue.substring(0, i);
                    value = keyValue.substring(i + 1);
                } else {
                    key = keyValue;
                }
                if (key.length() > 0) {
                    MapUtil.mergeValueInMap(params, key, value);
                }
            }

            // Second phase - look for _charset_ param and do the encoding
            Charset charset = Charset.forName(encoding);
            if (params.containsKey("_charset_")) {
                // The form contains a _charset_ param - When this is used together
                // with accept-charset, we can use _charset_ to extract the encoding.
                // PS: When rendering the view/form, _charset_ and accept-charset must be given the
                // same value - since only Firefox and sometimes IE actually sets it when Posting
                String providedCharset = params.get("_charset_")[0];
                // Must be sure the providedCharset is a valid encoding..
                try {
                    "test".getBytes(providedCharset);
                    charset = Charset.forName(providedCharset); // it works..
                } catch (Exception e) {
                    logger.debug("Got invalid _charset_ in form: " + providedCharset);
                    // lets just use the default one..
                }
            }

            // We're ready to decode the params
            Map<String, String[]> decodedParams = new LinkedHashMap<String, String[]>(params.size());
            for (Map.Entry<String, String[]> e : params.entrySet()) {
                String key = e.getKey();
                try {
                    key = Codec.decodeUrl(e.getKey(), charset);
                } catch (Throwable z) {
                    // Nothing we can do about, ignore
                }
                for (String value : e.getValue()) {
                    try {
                        MapUtil.mergeValueInMap(decodedParams, key, (value == null ? null : Codec.decodeUrl(value, charset)));
                    } catch (Throwable z) {
                        // Nothing we can do about, lets fill in with the non decoded value
                        MapUtil.mergeValueInMap(decodedParams, key, value);
                    }
                }
            }

            // add the complete body as a parameters
            if (!forQueryString) {
                decodedParams.put(ActionContext.REQ_BODY, new String[]{data});
            }

            return decodedParams;
        } catch (Result s) {
            // just pass it along
            throw s;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

}
