package act.test.verifier;

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

import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public class Exists extends Verifier {

    @Override
    public boolean verify(Object value) {
        boolean b = $.bool(initVal);
        return b == exists(value);
    }

    @Override
    protected List<String> aliases() {
        return C.list("notEmpty");
    }

    private boolean exists(Object value) {
        if (null == value) {
            return false;
        }
        if (value instanceof String) {
            return S.notEmpty((String) value);
        }
        return true;
    }
}
