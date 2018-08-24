package act.test.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

public interface TxScopeProvider {
    void enter();
    void rollback(Exception e);
    void commit();
    void clear();

    class DumbProvider implements TxScopeProvider {
        @Override
        public void enter() {
        }

        @Override
        public void rollback(Exception e) {
            throw E.asRuntimeException(e);
        }

        @Override
        public void commit() {
        }

        @Override
        public void clear() {
        }
    }
}
