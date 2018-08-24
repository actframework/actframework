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

import act.test.verifier.Exists;
import org.junit.Test;
import osgl.ut.TestBase;

public class ExistsTest extends TestBase {

    @Test
    public void nonNullObjectShallExists() {
        Exists e = new Exists();
        e.init(true);
        yes(e.verify(new Object()));
        e.init(false);
        no(e.verify(new Object()));
    }

    @Test
    public void nullObjectShallNotExists() {
        Exists e = new Exists();
        e.init("true");
        no(e.verify(null));
        e.init("false");
        yes(e.verify(null));
    }

    @Test
    public void nonEmptyStringIsExist() {
        Exists e = new Exists();
        e.init("true");
        yes(e.verify(" "));
        e.init("false");
        no(e.verify(" "));
    }

    @Test
    public void emptyStringShallNotExists() {
        Exists e = new Exists();
        e.init("true");
        no(e.verify(""));
        e.init("false");
        yes(e.verify(""));
    }

}
