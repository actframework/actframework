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

import act.Act;
import act.cli.CliContext;
import act.cli.CliSession;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;

import java.lang.annotation.Annotation;
import javax.enterprise.context.ApplicationScoped;

public abstract class CliHandlerBase extends $.F1<CliContext, Void> implements CliHandler {

    private boolean destroyed;

    @Override
    public final Void apply(CliContext context) throws NotAppliedException, $.Break {
        handle(context);
        return null;
    }

    @Override
    public boolean appliedIn(Act.Mode mode) {
        return true;
    }

    @Override
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        releaseResources();
    }

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    public String summary() {
        return "";
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}

    public void resetCursor(CliSession session) {
        session.removeCursor();
    }
}
