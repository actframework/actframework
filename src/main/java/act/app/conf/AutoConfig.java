package act.app.conf;

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

import act.app.event.SysEventId;

import java.lang.annotation.*;

/**
 * This annotation marks a class as auto configured class. E.g
 *
 * ```java
 * {@literal @}AutoConfig("foo") class Foo {
 *     public static final Const<String> name = $.constant("foo1");
 *     public static class Bar {
 *         public static final Const<Integer> limit = $.constant(100);
 *     }
 *     ...
 *     public void x() {
 *         System.out.println(Foo.name.get());
 *         System.out.println(Foo.Bar.limit.get());
 *     }
 * }
 * ```
 *
 * As the above classs `Foo` has been annotated with `AutoConfig("foo")`,
 * ActFramework will automatically configure the class by setting the
 * static field `name` and `Bar.limit` if the following configurations
 * exists:
 *
 * 1. foo.name=foo2
 * 2. foo.bar.limit=500
 *
 * With the above configuration, when calling new Foo().x(), it will print
 * out:
 *
 * ```
 * foo2
 * 500
 * ```
 *
 * instead of
 *
 * ```
 * foo1
 * 100
 * ```
 *
 * *Note* the static fields are populated on {@link SysEventId#START event},
 * meaning any logic in your application executed before app start shall NOT refer to
 * auto config class's configurable fields
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface AutoConfig {
    /**
     * define namespace of the configuration
     */
    String value() default "app";
}
