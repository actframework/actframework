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

import act.handler.builtin.controller.RequestHandlerProxy;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Mark an action handler method result can be cached
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheFor {

    /**
     * Application specify the ID of the CacheFor and use it
     * to reset cache
     */
    String id() default "";

    /**
     * Specify cache expiration time in seconds
     *
     * Default value: `60 * 60`, i.e. one hour
     *
     * @return the expiration time
     */
    int value() default 60 * 60;

    /**
     * Specify the keys to extract parameter/post variables to build the final
     * cache key.
     *
     * If not supplied then framework will try to iterate through all query
     * parameters to build the cache key
     *
     * @return the keys that should be used to build the final cache key
     */
    String[] keys() default {};

    /**
     * In some edge case (e.g. facebook post to app to get the landing page) POST
     * is treated as GET, we should allow cache the result in that case.
     *
     * default value: `false`
     *
     * @return `true` if enable cache on POST request
     */
    boolean supportPost() default false;

    /**
     * Specify it shall use `private` for `Cache-Control`
     *
     * default value: `false`.
     *
     * @return
     */
    boolean usePrivate() default false;

    /**
     * Suppress `Cache-Control` header setting.
     *
     * default value: `false`
     *
     * @return
     */
    boolean noCacheControl() default false;

    /**
     * Indicate Do not cache result into local server cache
     *
     * This is useful when the result is coming from an external
     * system that might or might not change. In which case we
     * really just want to calculate the result's etag and compare
     * it with request's `If-None-Match` header.
     *
     * Default value is `false`.
     */
    boolean eTagOnly() default false;

    /**
     * Set `no-cache` to `Cache-Control` header.
     *
     * Default value: the value of {@link #eTagOnly()}
     */
    boolean noCache() default false;

    @Singleton
    class Manager extends LogSupportedDestroyableBase {

        private Map<String, RequestHandlerProxy> proxyLookup = new HashMap<>();

        @Override
        protected void releaseResources() {
            proxyLookup.clear();
            proxyLookup = null;
        }

        public void register(String key, RequestHandlerProxy proxy) {
            RequestHandlerProxy existing = proxyLookup.put(key, proxy);
            E.illegalStateIf(null != existing, "proxy already registered with key[%s]: %s", key, proxy);
        }

        /**
         * Reset CacheFor cache for a request handler specified by controller class and
         * request handler method name.
         * @param controllerClass
         *      the host class of the handler method
         * @param requestHandlerName
         *      the request handler method name
         */
        public void resetCache(Class<?> controllerClass, String requestHandlerName) {
            String key = S.pathConcat(controllerClass.getName(), '.', requestHandlerName);
            resetCache(key);
        }

        /**
         * Reset CacheFor cache for a request handler specified
         * by {@link CacheFor#id()}.
         *
         * @param cacheForId
         *      the cacheFor id
         */
        public void resetCache(String cacheForId) {
            RequestHandlerProxy proxy = proxyLookup.get(cacheForId);
            if (null == proxy) {
                warn("Cannot find proxy by key: " + cacheForId);
            } else {
                proxy.resetCache();
            }
        }
    }
}
