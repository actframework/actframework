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

import java.io.File;

/**
 * The current running directory is the app base
 */
class SingleAppScanner extends AppScanner {

    @Override
    protected File[] appBases() {
        File file = currentDir();
        return new File[]{file};
    }

    private static File currentDir() {
        return new File(".");
    }

    @Override
    public AppScanner register(ProjectLayoutProbe probe) {
        return this;
    }

}
