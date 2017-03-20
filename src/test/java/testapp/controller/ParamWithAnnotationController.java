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

import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import playground.EmailBinder;
import playground.MyConstraint;

public class ParamWithAnnotationController {
    @Action(value = "/foo", methods = {H.Method.POST, H.Method.GET})
    public void bindNameChanged(@Param("bar") String foo) {}

    @Action("/x")
    public void defValPresented(@Param(defIntVal = 5) int x) {}

    @Action("/y")
    public void binderRequired(@Bind(EmailBinder.class) @MyConstraint(groups = {String.class, Integer.class}) String email) {}

}
