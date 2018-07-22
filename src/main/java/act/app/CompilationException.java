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

import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CompilationException extends ActAppException implements SourceInfo {

    private String problem;
    private File source;
    private Integer line;
    private Integer column;
    private Integer start;
    private Integer end;

    public CompilationException(String problem) {
        super(problem);
        this.problem = problem;
    }

    public CompilationException(File source, String problem, int line, int column, int start, int end) {
        super(problem);
        this.problem = problem;
        this.line = line;
        this.column = column;
        this.source = source;
        this.start = start;
        this.end = end;
    }

    @Override
    public String getErrorTitle() {
        return "Compilation error";
    }

    @Override
    public String getErrorDescription() {
        return S.fmt("The file <strong>%s</strong> could not be compiled.\nError raised is : <strong>%s</strong>", isSourceAvailable() ? source.getPath() : "", problem.replace("<", "&lt;"));
    }

    @Override
    public String fileName() {
        return source.getName();
    }

    @Override
    public List<String> lines() {
        String sourceCode = IO.readContentAsString(source);
        if (start != -1 && end != -1) {
            if (start.equals(end)) {
                sourceCode = sourceCode.substring(0, start + 1) + "↓" + sourceCode.substring(end + 1);
            } else {
                sourceCode = sourceCode.substring(0, start) + "\000" + sourceCode.substring(start, end + 1) + "\001" + sourceCode.substring(end + 1);
            }
        }
        return Arrays.asList(sourceCode.split("\n"));
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
        return source != null && line != null;
    }

    public Integer getSourceStart() {
        return start;
    }

    public Integer getSourceEnd() {
        return end;
    }
}
