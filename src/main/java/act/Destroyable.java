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

import act.handler.builtin.controller.RequestHandlerProxy;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import org.osgl.util.IO;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;

public interface Destroyable {
    void destroy();

    boolean isDestroyed();

    Class<? extends Annotation> scope();

    enum Util {
        ;

        /**
         * Helper method to destroy all elements in a in a {@link Destroyable} collection
         *
         * @param col the collection contains {@link Destroyable} elements
         * @deprecated use {@link #destroyAll(Collection, Class)} instead
         */
        public static void destroyAll(Collection<? extends Destroyable> col) {
            destroyAll(col, null);
        }

        public static void destroyAll(Collection<? extends Destroyable> col, Class<? extends Annotation> scope) {
            for (Destroyable e : col) {
                if (inScope(e, scope)) {
                    e.destroy();
                    if (e instanceof Closeable) {
                        IO.close((Closeable) e);
                    }
                }
            }
        }

        /**
         * Helper method to destroy all {@link Destroyable} elements,
         * and close all {@link Closeable} elements in the collection specified
         *
         * @param col the collection might contains {@link Destroyable} and
         *            {@link Closeable} elements
         */
        @Deprecated
        public static void tryDestroyAll(Collection<?> col) {
            tryDestroyAll(col, null);
        }

        /**
         * Helper method to destroy all {@link Destroyable} elements,
         * and close all {@link Closeable} elements in the collection specified
         *
         * @param col   the collection might contains {@link Destroyable} and
         *              {@link Closeable} elements
         * @param scope specify the scope annotation.
         */
        public static void tryDestroyAll(Collection<?> col, Class<? extends Annotation> scope) {
            if (null == col) {
                return;
            }
            for (Object o : col) {
                if (inScope(o, scope)) {
                    try {
                        if (o instanceof Destroyable) {
                            ((Destroyable) o).destroy();
                        }
                    } catch (Exception e) {
                        Act.LOGGER.warn(e, "Error encountered destroying instance of %s", o.getClass().getName());
                        // keep destroy next one
                    }
                    if (o instanceof Closeable) {
                        IO.close((Closeable) o);
                    }
                }
            }

            //col.clear();
        }

        public static void tryDestroy(Object o) {
            tryDestroy(o, null);
        }

        public static void tryDestroy(Object o, Class<? extends Annotation> scope) {
            if (null == o) {
                return;
            }
            if (!inScope(o, scope)) {
                return;
            }
            if (o instanceof Destroyable) {
                ((Destroyable) o).destroy();
            }
            if (o instanceof Closeable) {
                IO.close((Closeable) o);
            }
        }

        private static boolean inScope(Object o, Class<? extends Annotation> scope) {
            if (null == o) {
                return false;
            }
            if (null == scope || scope == ApplicationScoped.class) {
                return true;
            }
            Class<?> c = o.getClass();
            // performance tune - most frequent types in ActionContext
            if (String.class == c || Method.class == c || Boolean.class == c) {
                return false;
            }
            if (RequestHandlerProxy.class == c || ReflectedHandlerInvoker.class == c) {
                return ApplicationScoped.class == scope;
            }
            if (Integer.class == c || Locale.class == c || Long.class == c || Double.class == c || Enum.class.isAssignableFrom(c)) {
                return false;
            }
            if (c.isAnnotationPresent(scope)) {
                return true;
            }
            if (scope == SessionScoped.class) {
                // RequestScoped is always inside Session scope
                return c.isAnnotationPresent(RequestScoped.class);
            }
            return false;
        }
    }
}
