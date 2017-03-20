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

import act.util.NoAutoRegister;
import org.osgl.storage.ISObject;
import org.osgl.util.IO;
import org.osgl.util.StringValueResolver;

import static act.data.annotation.ReadContent.ATTR_MERCY;

/**
 * Read content as string from resource URL
 */
@NoAutoRegister
public class ContentStringResolver extends StringValueResolver<String> {

    public static final ContentStringResolver INSTANCE = new ContentStringResolver();

    @Override
    public String resolve(String value) {
        try {
            ISObject sobj = SObjectResolver.INSTANCE.resolve(value);
            return null == sobj ? fallBack(value) : IO.readContentAsString(sobj.asInputStream());
        } catch (Exception e) {
            return fallBack(value);
        }
    }

    private String fallBack(String value) {
        Boolean mercy = attribute(ATTR_MERCY);
        if (null == mercy) {
            mercy = false;
        }
        return mercy ? value : null;
    }

}
