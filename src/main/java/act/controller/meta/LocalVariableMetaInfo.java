package act.controller.meta;

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

import act.asm.Label;
import org.osgl.util.E;
import org.osgl.util.S;

public class LocalVariableMetaInfo {
    private Label start;
    private Label end;
    private String name;
    private String type;
    private int index;

    public LocalVariableMetaInfo(int index, String name, String type, Label start, Label end) {
        E.NPE(name, start, end);
        this.index = index;
        this.name = name;
        this.type = type;
        this.start = start;
        this.end = end;
    }

    public Label start() {
        return start;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public int index() {
        return index;
    }

    @Override
    public String toString() {
        // print out the local variable className as
        // $index   $className   $start_label  $end_label
        return S.concat(S.string(index), "\t", S.string(name), "\t", S.string(start), "\t", S.string(end));
    }
}
