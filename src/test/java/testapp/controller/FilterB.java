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
import org.osgl.mvc.annotation.Before;
import org.osgl.mvc.annotation.With;
import testapp.util.Trackable;

/**
 * Created by luog on 1/03/2015.
 */
@With({FilterWithNoEffect.class, FilterF.class})
public class FilterB extends Trackable {

    @Before
    public static void before() {
        trackStatic("FilterB", "before");
    }

    @Before(except = "foo")
    public void beforeExceptFoo() {
        track("beforeExceptFoo");
    }

    @Before(except = "foo, bar")
    public void beforeExceptFooBar() {
        track("beforeExceptFooBar");
    }

    @Before(except = {"foo", "bar"})
    public void beforeExceptFooBar2() {
        track("beforeExceptFooBar2");
    }

    @Before(only = "foo")
    public void beforeOnlyFoo() {
        track("beforeOnlyFoo");
    }

    @Before(only = "foo, bar")
    public void beforeOnlyFooBar() {
        track("beforeOnlyFooBar");
    }

    @After(priority = 99)
    public void afterP99() {
        track("afterP99");
    }
}
