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

import java.util.List;

/**
 * Implemented by exceptions with srccode attachment
 */
public interface SourceInfo {
    String fileName();

    List<String> lines();

    Integer lineNumber();

    Integer column();

    boolean isSourceAvailable();

    abstract class Base implements SourceInfo {
        protected String fileName;
        protected List<String> lines;
        protected int lineNumber;
        protected int column = -1;

        @Override
        public String fileName() {
            return fileName;
        }

        @Override
        public List<String> lines() {
            return lines;
        }

        @Override
        public Integer lineNumber() {
            return lineNumber;
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
}
