package act.inject.param;

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

import act.db.AdaptiveRecord;
import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.util.S;

import java.util.Set;

class AdaptiveRecordLoader extends PojoLoader {
    public AdaptiveRecordLoader(ParamKey key, BeanSpec spec, ParamValueLoaderService service) {
        super(key, spec, service);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        AdaptiveRecord ar = (AdaptiveRecord) super.load(bean, context, noDefaultValue);
        Set<String> loadedFields = fieldLoaders.keySet();
        String prefix = S.concat(bindName(), ".");
        Set<String> paramKeys = context.paramKeys();
        boolean allowIgnoreNamespace = context.isAllowIgnoreParamNamespace();
        for (String paramKey : paramKeys) {
            if (paramKey.startsWith(prefix)) {
                String field = S.afterFirst(paramKey, prefix);
                if (S.notBlank(field) && !loadedFields.contains(field)) {
                    if (field.contains(".")) {
                        warn("AdaptiveRecordLoader does not support nested structure");
                        continue;
                    }
                    ar.putValue(field, context.paramVal(paramKey));
                }
            } else if (allowIgnoreNamespace) {
                String field = paramKey;
                if (S.notBlank(field) && !loadedFields.contains(field)) {
                    if (field.contains(".")) {
                        warn("AdaptiveRecordLoader does not support nested structure");
                        continue;
                    }
                    ar.putValue(field, context.paramVal(paramKey));
                }
            }
        }
        return ar;
    }
}
