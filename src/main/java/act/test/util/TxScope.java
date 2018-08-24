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

import act.Act;

public class TxScope {

    private static TxScopeProvider provider;
    static {
        try {
            provider = Act.getInstance("act.db.sql.test.SqlTxScope");
        } catch (Throwable e) {
            provider = new TxScopeProvider.DumbProvider();
        }
    }

    public static void enter() {
        provider.enter();
    }
    public static void rollback(Exception e) {
        provider.rollback(e);
    }
    public static void commit() {
        provider.commit();
    }
    public static void clear() {
        provider.clear();
    }

}
