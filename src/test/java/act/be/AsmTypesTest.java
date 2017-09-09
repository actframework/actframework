package act.be;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
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

import act.ActTestBase;
import act.app.ActionContext;
import act.asm.Type;
import act.util.AsmTypes;
import org.junit.Test;

import java.lang.reflect.Method;

public class AsmTypesTest extends ActTestBase {

    @Test
    public void testMethodDescWithoutReturnType() throws Exception {
        Method m = AsmTypesTest.class.getDeclaredMethod("testMethodDescWithoutReturnType");
        Type mt = Type.getType(m);
        eq(mt.getDescriptor(), AsmTypes.methodDesc(Void.class));
    }

    @Test
    public void testMethodDescWithParamAndReturnType() throws Exception {
        Method m = ActionContext.class.getDeclaredMethod("paramVal", String.class);
        Type mt = Type.getType(m);
        eq(mt.getDescriptor(), AsmTypes.methodDesc(String.class, String.class));
    }

}
