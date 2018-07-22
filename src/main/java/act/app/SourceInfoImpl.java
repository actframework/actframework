package act.app;

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

import java.util.List;

public class SourceInfoImpl implements SourceInfo {

    private Source source;
    private int line;
    private int column;

    public SourceInfoImpl(Source source, int line) {
        this(source, line, -1);
    }

    public SourceInfoImpl(Source source, int line, int column) {
        E.NPE(source);
        this.source = source;
        this.line = line;
        this.column = column;
    }

    @Override
    public String fileName() {
        return source.file().getName();
    }

    @Override
    public List<String> lines() {
        return source.lines();
    }

    @Override
    public Integer lineNumber() {
        return line;
    }

    @Override
    public Integer column() {
        return column;
    }

    @Override
    public boolean isSourceAvailable() {
        return true;
    }
}
