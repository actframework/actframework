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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a controller action handler method is virtual.
 *
 * If an action handler method is virtual then framework will register
 * the route for sub classes. e.g
 *
 * <pre>
 * // The base controller provides `doIt` handler
 * public abstract class BaseController {
 *      protected abstract doPlumbingWork() {}
 *     @GetAction("doIt")
 *     @Virtual
 *     public void doIt() {
 *         doPlumbingWork();
 *         ...
 *     }
 * }
 * </pre>
 *
 * <pre>
 * // The foo implementation
 * @Controller("/foo")
 * public class FooController extends BaseController {
 *      protected void doPlumbingWork() {...}
 * }
 * </pre>
 *
 * <pre>
 * // The bar implementation
 * @Controller("/bar")
 * public class BarController extends BaseController {
 *      protected void doPlumbingWork() {...}
 * }
 * </pre>
 *
 * Because method `doIt()` in `BaseController` is marked as `@Virtual`, the framework will add the following routes
 * to routing table:
 *
 * <pre>
 * GET /foo/doIt pkg.to.FooController.doIt
 * GET /bar/doIt pkg.to.BarController.doIt
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Virtual {
}
