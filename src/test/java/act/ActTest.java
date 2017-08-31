package act;

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

import org.junit.Test;
import org.osgl.$;
import testapp.Main;

public class ActTest extends TestBase {

    @Test
    public void testGetAppNameAndPackage() {
        String className = ActTest.class.getName();
        $.Var<String> appNameHolder = $.var();
        $.Var<String> pkgNameHolder = $.var();
        Act.getAppNameAndPackage(className, appNameHolder, pkgNameHolder);
        eq("Act Test", appNameHolder.get());
        eq("act", pkgNameHolder.get());
    }

    @Test
    public void testGetAppNameAndPackageWithNestedEntryClass() {
        String className = Main.Entry.class.getName();
        $.Var<String> appNameHolder = $.var();
        $.Var<String> pkgNameHolder = $.var();
        Act.getAppNameAndPackage(className, appNameHolder, pkgNameHolder);
        eq("Testapp", appNameHolder.get());
        eq("testapp", pkgNameHolder.get());
    }
}
