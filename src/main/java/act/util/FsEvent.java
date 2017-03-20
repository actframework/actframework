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

import org.osgl.util.C;

import java.util.Collection;
import java.util.List;

public class FsEvent {

    public enum Kind {
        CREATE, DELETE, MODIFY
    }

    private List<String> paths;
    private Kind kind;

    public FsEvent(Kind kind, String... paths) {
        this.paths = C.listOf(paths);
        this.kind = kind;
    }

    public FsEvent(Kind kind, Collection<String> paths) {
        this.paths = C.list(paths);
        this.kind = kind;
    }

    public List<String> paths() {
        return this.paths;
    }

    public Kind kind() {
        return this.kind;
    }

}
