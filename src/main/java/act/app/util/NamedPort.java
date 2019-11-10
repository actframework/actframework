package act.app.util;

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

import org.osgl.util.E;
import org.osgl.util.S;

public class NamedPort implements Comparable<NamedPort> {

    public static final String DEFAULT = "default";

    private String name;
    private int port;

    public NamedPort(String name, int port) {
        E.NPE(name);
        E.illegalArgumentIf(port < 0);
        this.name = name;
        this.port = port;
    }

    public String name() {
        return name;
    }

    public int port() {
        return port;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NamedPort) {
            NamedPort that = (NamedPort) obj;
            return S.eq(that.name, name);
        }
        return false;
    }

    @Override
    public int compareTo(NamedPort o) {
        return o.name.compareTo(name);
    }

    @Override
    public String toString() {
        return S.fmt("%s[:%s]", name, port);
    }
}
