package act.plugin;

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

import act.cli.Command;
import act.cli.Optional;
import act.util.PropertySpec;
import org.osgl.Lang;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

/**
 * Provides admin interface to access Plugin data info
 */
public class PluginAdmin {

    @Command(name = "act.plugin.list,act.plugin,act.plugins", help = "list plugins")
    @PropertySpec("this as Plugin")
    public List<String> list(
            @Optional("sort alphabetically") boolean sort,
            @Optional("filter plugin") final String q
    ) {
        C.List<String> l =  C.list(Plugin.InfoRepo.plugins());
        if (S.notBlank(q)) {
            l = l.filter(new Lang.Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return s.toLowerCase().contains(q) || s.matches(q);
                }
            });
        }
        return sort ? l.sorted() : l;
    }

}
