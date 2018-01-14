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
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.Map;

// Disclaim, major logic of this component come from PlayFramework 1.3's DataParser
public abstract class RequestBodyParser {

    protected static final Logger logger = L.get(RequestBodyParser.class);

    private static Map<H.Format, RequestBodyParser> parsers = C.Map(
            H.Format.FORM_MULTIPART_DATA, new ApacheMultipartParser(),
            H.Format.FORM_URL_ENCODED, new UrlEncodedParser(),
            H.Format.JSON, TextParser.INSTANCE,
            H.Format.XML, TextParser.INSTANCE,
            H.Format.CSV, TextParser.INSTANCE
    );

    public static RequestBodyParser get(H.Request req) {
        H.Format contentType = req.contentType();
        RequestBodyParser parser = parsers.get(contentType);
        if (null == parser) {
            parser = TextParser.INSTANCE;
        }
        return parser;
    }

    public abstract Map<String, String[]> parse(ActionContext context);

}

