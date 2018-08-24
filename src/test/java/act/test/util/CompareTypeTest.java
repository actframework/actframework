package act.test.util;

/*-
 * #%L
 * ACT E2E Plugin
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

import static act.test.verifier.Compare.Type.*;

import org.junit.Test;
import osgl.ut.TestBase;

public class CompareTypeTest extends TestBase {

    @Test
    public void testGT() {
        yes(GT.applyTo(1, 5));
        yes(GT.applyTo("a", "z"));
        no(GT.applyTo(1, 1));
        no(GT.applyTo("a", "a"));
    }

    @Test
    public void testGTE() {
        yes(GTE.applyTo(1, 5));
        yes(GTE.applyTo("a", "z"));
        yes(GTE.applyTo(1, 1));
        yes(GTE.applyTo("a", "a"));
        no(GTE.applyTo(10, 1));
        no(GTE.applyTo("z", "a"));
    }


    @Test
    public void testLT() {
        yes(LT.applyTo(5, 1));
        yes(LT.applyTo("z", "a"));
        no(LT.applyTo(1, 1));
        no(LT.applyTo("a", "a"));
    }

    @Test
    public void testLTE() {
        yes(LTE.applyTo(5, 1));
        yes(LTE.applyTo("z", "a"));
        yes(LTE.applyTo(1, 1));
        yes(LTE.applyTo("a", "a"));
        no(LTE.applyTo(1, 5));
        no(LTE.applyTo("a", "z"));
    }

}
