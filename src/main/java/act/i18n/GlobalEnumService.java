package act.i18n;

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

import act.inject.DefaultValue;
import org.osgl.mvc.annotation.GetAction;

import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GlobalEnumService {

    @Inject
    EnumLookupCache cache;

    @GetAction("i18n/enum/")
    public Map lookupTable(@DefaultValue("false") boolean outputProperties, Locale locale) {
        return outputProperties ? cache.withProperties(locale) : cache.withoutProperties(locale);
    }

}
