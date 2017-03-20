package act.handler;

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

import act.app.App;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;

public abstract class RequestHandlerResolverBase extends $.F2<String, App, RequestHandler> implements RequestHandlerResolver {

    private boolean destroyed;

    @Override
    public RequestHandler apply(String s, App app) throws NotAppliedException, $.Break {
        return resolve(s, app);
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        releaseResources();
    }

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {

    }
}
