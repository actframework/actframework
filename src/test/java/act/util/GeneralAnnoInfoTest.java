package act.util;

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
import act.asm.Type;
import org.junit.Before;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;

import javax.validation.constraints.NotNull;

public class GeneralAnnoInfoTest extends ActTestBase {
    private GeneralAnnoInfo actionInfo;
    private GeneralAnnoInfo notNullInfo;

    @Before
    public void setup() {
        actionInfo = new GeneralAnnoInfo(Type.getType(Action.class));
        actionInfo.putListAttribute("value", "/foo");
        actionInfo.putListAttribute("methods", H.Method.GET);
        actionInfo.putListAttribute("methods", H.Method.POST);

        notNullInfo = new GeneralAnnoInfo(Type.getType(NotNull.class));
        notNullInfo.putListAttribute("group", Action.class);
    }

    @Test
    public void annotationProxyTest() {
        Action action =  actionInfo.toAnnotation();
        eq(action.value(), new String[]{"/foo"});
        eq(action.methods(), new H.Method[]{H.Method.GET, H.Method.POST});

        NotNull notNull = notNullInfo.toAnnotation();
        eq(notNull.message(), "{javax.validation.constraints.NotNull.message}");
    }

}
