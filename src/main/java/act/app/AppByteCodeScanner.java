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

import act.util.ByteCodeVisitor;

import java.util.Map;
import java.util.Set;

/**
 * A {@code AppByteCodeScanner} scans application bytecode
 */
public interface AppByteCodeScanner extends AppCodeScanner {
    /**
     * Returns the {@link ByteCodeVisitor}
     */
    ByteCodeVisitor byteCodeVisitor();

    /**
     * After visiting a class bytecode, this method will be called
     * to check if there are dependency class needs to be scanned
     * again
     */
    Map<Class<? extends AppByteCodeScanner>, Set<String>> dependencyClasses();

    /**
     * Called when scanning for one class finished
     */
    void scanFinished(String className);
}
