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
import org.osgl.$;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.util.S;

import java.io.File;

/**
 * Resolve file uploads
 */
public class FileBinder extends Binder<File> {

    @Override
    public File resolve(File file, String s, ParamValueProvider paramValueProvider) {
        ActionContext ctx = $.cast(paramValueProvider);
        if (ctx.req().url().startsWith("/~/cmd/run")) {
            String fileName = ctx.paramVal(s);
            if (S.isBlank(fileName)) {
                return null;
            }
            return new File(fileName);
        }
        ISObject sobj = ctx.upload(s);
        return null == sobj ? null : sobj.asFile();
    }
}
