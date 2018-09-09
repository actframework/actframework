package act.db.tx;

/*-
 * #%L
 * ACT SQL Common Module
 * %%
 * Copyright (C) 2015 - 2018 ActFramework
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

public class TxInfo {

    public boolean readOnly;
    public boolean withinTxScope;
    public TxScopeEventListener listener;

    public TxInfo(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void enterTxScope() {
        this.withinTxScope = true;
    }

    public void exitTxScope() {
        if (null != listener) {
            listener.exit();
            listener = null;
        }
        this.withinTxScope = false;
    }

    public void exitTxScope(Throwable cause) {
        if (null != listener) {
            listener.rollback(cause);
            listener = null;
        }
        this.withinTxScope = false;
    }

}
