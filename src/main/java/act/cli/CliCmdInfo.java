package act.cli;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.util.Map;
import java.util.Set;

public class CliCmdInfo implements Comparable<CliCmdInfo> {

    public String name;
    public String shortcut;
    public String help = "<no help message>";
    public Map<String, String> params;
    public Set<String> multiLinesParams;
    public boolean hasReturnValue;
    public boolean requireCliContext;

    public String nameAndShortcut() {
        return S.buffer(name).a("(").a(shortcut).a(")").toString();
    }

    public int nameAndShortcutLen() {
        return nameAndShortcut().length();
    }

    public int helpLen() {
        return help.length();
    }

    public String httpName() {
        return Keyword.of(name).kebabCase();
    }

    @Override
    public int compareTo(CliCmdInfo o) {
        return name.compareTo(o.name);
    }

    public boolean isMultiLines(String paramName) {
        return multiLinesParams.contains(paramName);
    }

}
