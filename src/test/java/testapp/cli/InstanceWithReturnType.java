package testapp.cli;

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
import act.cli.Required;
import act.util.PropertySpec;

import java.util.List;

public class InstanceWithReturnType {

    private String s;

    @Command("user.list")
    @PropertySpec("fn as firstName,ln as lastName")
    public List<String> getUserList(
            @Required(lead = "-i,--id", group = "group1") String id,
            boolean b,
            @Optional(defVal = "-1") int limit,
            long l
    ) {
        return null;
    }

}
