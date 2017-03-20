package testapp.controller;

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

import org.osgl.mvc.annotation.After;
import org.osgl.mvc.annotation.Finally;
import org.osgl.mvc.annotation.With;
import testapp.util.Trackable;

@With(FilterWithNoEffect.class)
public abstract class FilterF extends Trackable {

    @Finally(priority = 10)
    public static void f1() {
        trackStatic("FilterF", "f1");
    }

    @After(priority = 3)
    public static void afterP3() {
        trackStatic("FilterF", "afterP3");
    }
}
