package act.apidoc.sampledata;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.apidoc.SampleDataProvider;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.net.URL;
import java.util.List;

public abstract class StringListProvider<T> extends SampleDataProvider<T> {

    private List<String> stringList;

    protected String listName() {
        String fileName = getClass().getSimpleName().toLowerCase();
        if (fileName.endsWith("provider")) {
            fileName = fileName.substring(0, fileName.length() - 8);
        }
        return fileName;
    }

    public StringListProvider() {
        if (Act.isProd()) {
            return;
        }
        String fileName = listName() + ".list";
        ClassLoader cl = Act.app().classLoader();
        URL url = cl.getResource("sampledata/" + fileName);
        if (null != url) {
            stringList = readUrl(url);
        } else {
            // try built-in resource
            url = cl.getResource("sampledata/~act/" + fileName);
            if (null != url) {
                stringList = readUrl(url);
            } else {
                warn("Cannot find the string list: " + fileName.toLowerCase() + ".list");
            }
        }
    }

    private List<String> readUrl(URL url) {
        return C.list(IO.read(url).toLines()).filter(S.F.NOT_BLANK);
    }

    protected String randomStr() {
        return null == stringList || stringList.isEmpty() ? S.random() : $.random(stringList);
    }
}
