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

import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;

public class In extends Verifier {

    private List<String> list;

    @Override
    public void init(Object param) {
        E.npeIf(null == param);
        String s = param.toString();
        list = S.split(s, S.COMMON_SEP);
    }

    @Override
    public boolean verify(Object value) {
        if (null == value) {
            return false;
        }
        String s = value.toString();
        return list.contains(s);
    }
}
