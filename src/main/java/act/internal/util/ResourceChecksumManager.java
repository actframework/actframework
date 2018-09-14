package act.internal.util;

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
import act.crypto.AppCrypto;
import act.util.LogSupportedDestroyableBase;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ResourceChecksumManager extends LogSupportedDestroyableBase {

    @Inject
    private AppCrypto crypto;

    // map path to checksum
    private Map<String, $.Val<String>> checksums = new HashMap<>();

    @Override
    protected void releaseResources() {
        checksums.clear();
        crypto = null;
    }

    public String checksumOf(String path) {
        E.illegalArgumentIf((path.startsWith("http:") || path.startsWith("//")));
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.contains("?")) {
            path = S.beforeFirst(path, "?");
        }
        $.Val<String> bag = checksums.get(path);
        if (null == bag) {
            InputStream is = Act.app().classLoader().getResourceAsStream(path);
            String checksum = null == is ? null : crypto.checksum(is);
            bag = $.val(checksum);
            checksums.put(path, bag);
        }
        return bag.get();
    }

}
