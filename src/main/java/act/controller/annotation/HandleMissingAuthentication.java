package act.controller.annotation;

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

import act.Act;
import act.util.MissingAuthenticationHandler;
import act.util.RedirectToLoginUrl;
import act.util.ReturnUnauthorized;
import org.osgl.util.S;

import java.lang.annotation.*;

/**
 * Specify how to handle the case when interact user not authenticated
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface HandleMissingAuthentication {

    enum Option {
        /**
         * redirect to login URL
         */
        REDIRECT() {
            @Override
            protected MissingAuthenticationHandler createHandler(String custom) {
                return S.blank(custom) ? Act.getInstance(RedirectToLoginUrl.class) : new RedirectToLoginUrl(custom);
            }
        },

        /**
         * send 401 response
         */
        REJECT() {
            @Override
            protected MissingAuthenticationHandler createHandler(String custom) {
                return Act.getInstance(ReturnUnauthorized.class);
            }
        },

        /**
         * Customized solution
         */
        CUSTOM() {
            @Override
            protected MissingAuthenticationHandler createHandler(String custom) {
                return Act.getInstance(custom);
            }
        };

        private volatile MissingAuthenticationHandler realHandler;

        public MissingAuthenticationHandler handler(String custom) {
            if (null == realHandler) {
                synchronized (this) {
                    if (null == realHandler) {
                        realHandler = createHandler(custom);
                    }
                }
            }
            return realHandler;
        }

        protected abstract MissingAuthenticationHandler createHandler(String custom);
    }

    /**
     * Specify the option to handle missing authentication case
     * @return the way to deal with missing authentication
     */
    Option value();

    /**
     * If the {@link #value()} is {@link Option#CUSTOM}, this
     * field specifies the customer implementation.
     *
     * If the {@link #value()} is {@link Option#REDIRECT} and this
     * field is provided, then it specifies the login URL
     *
     *
     * @return the custom implementation or login URL
     */
    String custom() default "";
}
