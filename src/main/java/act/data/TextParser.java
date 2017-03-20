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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TextParser extends RequestBodyParser {

    public static final TextParser INSTANCE = new TextParser();

    @Override
    public Map<String, String[]> parse(ActionContext context) {
        H.Request req = context.req();
        InputStream is = req.inputStream();
        try {
            Map<String, String[]> params = new HashMap<String, String[]>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            byte[] data = os.toByteArray();
            params.put(ActionContext.REQ_BODY, data.length == 0 ? null : new String[] {new String(data, req.characterEncoding())});
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
