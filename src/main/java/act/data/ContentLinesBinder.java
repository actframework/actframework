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

import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.IO;

import java.util.List;

import static act.data.annotation.ReadContent.ATTR_MERCY;

/**
 * Read content lines from resource URL
 */
public class ContentLinesBinder extends Binder<List<String>> {

    public static final ContentLinesBinder INSTANCE = new ContentLinesBinder();

    @Override
    public List<String> resolve(List<String> bean, String model, ParamValueProvider params) {
        try {
            ISObject sobj = SObjectBinder.INSTANCE.resolve(null, model, params);
            return null == sobj ? fallBack(model, params) : IO.readLines(sobj.asInputStream());
        } catch (Exception e) {
            return fallBack(model, params);
        }
    }

    private List<String> fallBack(String model, ParamValueProvider params) {
        Boolean mercy = attribute(ATTR_MERCY);
        if (null == mercy) {
            mercy = false;
        }
        if (mercy) {
            String val = params.paramVal(model);
            return null == val ? C.<String>list() : C.list(val);
        }
        return C.list();
    }

}
