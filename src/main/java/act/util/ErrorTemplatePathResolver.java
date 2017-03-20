package act.util;

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
import org.osgl.http.H;
import org.osgl.util.S;

import static org.osgl.http.H.Format.*;

public interface ErrorTemplatePathResolver {
    String resolve(int code, H.Format format);

    class DefaultErrorTemplatePathResolver implements ErrorTemplatePathResolver {
        @Override
        public String resolve(int code, H.Format fmt) {
            String suffix;
            if (JSON == fmt || HTML == fmt || XML == fmt) {
                suffix = fmt.name();
            } else {
                suffix = TXT.name();
            }
            return Act.isProd() || "json".equals(suffix) ? S.fmt("/error/e%s.%s", code, suffix) : S.fmt("/error/dev/e%s.%s", code, suffix);
        }
    }
}
