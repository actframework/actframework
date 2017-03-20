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

/**
 * A {@code SourceCodeScanner} scan source code.
 * This function is required only when application is running
 * at dev mode
 */
public interface AppSourceCodeScanner extends AppCodeScanner {

    /**
     * Visit the source code line. The implementation shall
     * set the internal state by inspecting the line
     * @param lineNumber
     * @param line
     * @param className
     */
    void visit(int lineNumber, String line, String className);

    /**
     * After scanning of a certain {@link Source} has been
     * finished, framework will call this method to check if
     * further byte code scanning is needed on the
     * class
     * @return {@code true} if it needs further bytecode
     * scanning on the class
     */
    boolean triggerBytecodeScanning();
}
