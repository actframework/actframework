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

import act.app.App;
import act.asm.ClassVisitor;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.E;

public abstract class AppByteCodeEnhancer<T extends AppByteCodeEnhancer> extends AsmByteCodeEnhancer<T> {
    protected static Logger logger = LogManager.get(App.class);
    protected App app;

    protected AppByteCodeEnhancer($.Predicate<String> targetClassPredicate) {
        super(targetClassPredicate);
    }

    protected AppByteCodeEnhancer($.Predicate<String> targetClassPredicate, ClassVisitor cv) {
        super(targetClassPredicate, cv);
    }

    public AppByteCodeEnhancer app(App app) {
        E.NPE(app);
        this.app = app;
        return this;
    }

}
