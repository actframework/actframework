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
 * Capture commonality between {@link AppSourceCodeScanner} and {@link AppByteCodeScanner}
 */
public interface AppCodeScanner {

    void setApp(App app);

    /**
     * Reset the scanner internal state to start an new scanning session.
     * Returns {@code false} if the {@link Source} is
     * not subject to scanning as per class name
     */
    boolean start(String className);
}
