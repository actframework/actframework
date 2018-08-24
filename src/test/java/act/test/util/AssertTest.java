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

import act.test.verifier.Eq;
import org.junit.Test;
import org.osgl.$;
import org.rythmengine.utils.S;
import osgl.ut.TestBase;

public class AssertTest extends TestBase {

    @Test
    public void cloneShallNotBeIdenticalToOrigin() {
        Eq eq = new Eq();
        eq.init(S.random());
        Eq clone = $.cloneOf(eq);
        notSame(eq, clone);
        eq(eq, clone);
    }

}
