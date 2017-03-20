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
import org.osgl.util.C;

import java.util.List;

/**
 * Provides admin interface to access Plugin data info
 */
public class PluginAdmin {

    @Command(name = "act.plugin.list", help = "list plugins")
    @PropertySpec("this as Plugin")
    public List<String> list(
            @Optional("sort alphabetically") boolean sort
    ) {
        C.List<String> l =  C.list(Plugin.InfoRepo.plugins());
        return sort ? l.sorted() : l;
    }

}
