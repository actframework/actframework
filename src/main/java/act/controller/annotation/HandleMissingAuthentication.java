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
import act.app.ActionContext;
import act.util.MissingAuthenticationHandler;
import act.util.RedirectToLoginUrl;
import act.util.ReturnUnauthorized;
import org.osgl.mvc.result.Result;

/**
 * Specify how to handle the case when interact user not authenticated
 */
public @interface HandleMissingAuthentication {

    enum Option implements MissingAuthenticationHandler {
        /**
         * redirect to login URL
         */
        REDIRECT(RedirectToLoginUrl.class),

        /**
         * send 401 response
         */
        REJECT(ReturnUnauthorized.class);

        private volatile MissingAuthenticationHandler realHandler;
        private Class<? extends MissingAuthenticationHandler> realHandlerType;

        Option(Class<? extends MissingAuthenticationHandler> realHandler) {
            this.realHandlerType = realHandler;
        }

        private MissingAuthenticationHandler realHandler() {
            if (null == realHandler) {
                synchronized (this) {
                    if (null == realHandler) {
                        realHandler = Act.getInstance(realHandlerType);
                    }
                }
            }
            return realHandler;
        }

        @Override
        public Result result(ActionContext context) {
            return realHandler().result(context);
        }
    }

    /**
     * Specify the option to handle missing authentication case
     * @return the way to deal with missing authentication
     */
    Option value();

}
