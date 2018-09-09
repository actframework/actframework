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

import org.osgl.util.E;

public class TxContext {

    private static final ThreadLocal<TxInfo> info_ = new ThreadLocal<>();

    public static boolean readOnly() {
        TxInfo info = info_.get();
        E.illegalStateIf(null == info);
        return info.readOnly;
    }

    public static boolean readOnly(boolean defaultReadOnly) {
        TxInfo info = info_.get();
        return null == info ? defaultReadOnly : info.readOnly;
    }

    public static boolean withinTxScope() {
        TxInfo info = info_.get();
        return null != info && info.withinTxScope;
    }

    public static TxInfo enterTxScope(boolean readonly) {
        TxInfo info = info_.get();
        // let's assume the semantic is SUPPORT, ie. when tx exists then use it
        // otherwise create it.
        // E.illegalStateIf(null != info && info.withinTxScope);
        if (null == info) {
            info = new TxInfo(readonly);
            info_.set(info);
        } else {
            info.readOnly = readonly;
        }
        info.enterTxScope();
        return info;
    }

    public static void exitTxScope() {
        TxInfo info = info_.get();
        E.illegalStateIf(null == info || !info.withinTxScope, "Not in a TX scope");
        info.exitTxScope();
    }

    public static void exitTxScope(Throwable cause) {
        TxInfo info = info_.get();
        E.illegalStateIf(null == info || !info.withinTxScope, "Not in a TX scope");
        info.exitTxScope(cause);
    }

    public static void clear() {
        TxInfo info = info_.get();
        if (null == info) {
            return;
        }
        try {
            if (info.withinTxScope) {
                info.exitTxScope();
            }
        } finally {
            info_.remove();
        }
    }

    public static void reset() {
        info_.remove();
    }

    public static TxInfo info() {
        return info_.get();
    }

}
