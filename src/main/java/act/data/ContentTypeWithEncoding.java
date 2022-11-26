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

import org.osgl.util.S;

class ContentTypeWithEncoding {
    public final String contentType;
    public final String encoding;

    public ContentTypeWithEncoding(String contentType, String encoding) {
        this.contentType = contentType;
        this.encoding = encoding;
    }

    public static ContentTypeWithEncoding parse(String contentType) {
        if( contentType == null ) {
            return new ContentTypeWithEncoding("text/html", null);
        } else {
            String[] contentTypeParts = contentType.split(";");
            String _contentType = contentTypeParts[0].trim().toLowerCase();
            String _encoding = null;
            // check for encoding-info
            if (contentTypeParts.length >= 2) {
                String[] encodingInfoParts = contentTypeParts[1].split(("="));
                if (encodingInfoParts.length == 2 && encodingInfoParts[0].trim().equalsIgnoreCase("charset")) {
                    // encoding-info was found in request
                    _encoding = encodingInfoParts[1].trim();

                    if (S.notBlank(_encoding) &&
                            ((_encoding.startsWith("\"") && _encoding.endsWith("\""))
                                    || (_encoding.startsWith("'") && _encoding.endsWith("'")))
                            ) {
                        _encoding = _encoding.substring(1, _encoding.length() - 1).trim();
                    }
                }
            }
            return new ContentTypeWithEncoding(_contentType, _encoding);
        }
    }
}

