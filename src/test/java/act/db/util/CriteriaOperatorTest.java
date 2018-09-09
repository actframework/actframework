package act.db.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import static act.db.CriteriaOperator.BuiltIn.*;

import act.db.CriteriaOperator;
import org.junit.Test;

public class CriteriaOperatorTest extends CriteriaTestBase {

    @Test
    public void verifyBuiltInOperatorConstants() {
        isPair(NOT_BETWEEN, BETWEEN);
        isPair(NOT_EXISTS, EXISTS);
        isPair(NE, EQ);
        isPair(UNLIKE, LIKE);
        isPair(GT, LTE);
        isPair(GTE, LT);
        isPair(IN, NIN);
    }

    private void isPair(CriteriaOperator a, CriteriaOperator b) {
        same(a, b.negate());
        same(b, a.negate());
    }
}
