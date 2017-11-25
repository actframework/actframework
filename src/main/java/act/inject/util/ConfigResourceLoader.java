package act.inject.util;

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
import org.osgl.util.E;
import org.osgl.util.S;

public class ConfigResourceLoader extends ResourceLoader {
    @Override
    protected void initialized() {
        String path = (String) options.get("value");
        E.unexpectedIf(S.blank(path), "resource path not specified");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.startsWith("config/")) {
            path = path.substring(7);
        }
        load(profileConfig(path), spec);
        if (null == resource) {
            load(commonConfig(path), spec);
        }
        if (null == resource) {
            load(confConfig(path), spec);
        }
        if (null == resource) {
            load(path, spec);
        }
    }

    private String profileConfig(String path) {
        return S.concat("conf/", Act.profile(), "/", path);
    }

    private String commonConfig(String path) {
        return S.concat("conf/common/", path);
    }

    private String confConfig(String path) {
        return S.concat("conf/", path);
    }

}
