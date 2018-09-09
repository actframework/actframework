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

import act.asm.Opcodes;

public class TxScopeHelper implements Opcodes {

    /**
     * Entering an enhanced transactional method.
     */
    public static void enter(TxInfo txInfo) {
        TxContext.enterTxScope(txInfo.readOnly);
    }

    /**
     * Exiting an enhanced transactional method.
     */
    public static void exit(Object returnOrThrowable, int opCode) {
        if (ATHROW == opCode) {
            TxContext.exitTxScope((Throwable) returnOrThrowable);
        } else {
            TxContext.exitTxScope();
        }
    }

}
