package act.controller.meta;

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
import act.app.App;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;

/**
 * Unlike other interceptors (Before/After/Finally), Catch interceptor
 * has a special attribute: value, the exception class. Only when the
 * exception thrown out is instance of the class or subclass of the class,
 * the catch interceptor will be executed
 */
public class CatchMethodMetaInfo extends InterceptorMethodMetaInfo {
    private static final List<String> CATCH_THROWABLE = C.list(Throwable.class.getName());
    private List<String> targetExceptionClassNames = CATCH_THROWABLE;
    private volatile Boolean paramTypeAnalyzed;

    protected CatchMethodMetaInfo(CatchMethodMetaInfo copy, ControllerClassMetaInfo clsInfo) {
        super(copy, clsInfo);
        this.targetExceptionClassNames = copy.targetExceptionClassNames;
    }

    public CatchMethodMetaInfo(ControllerClassMetaInfo clsInfo) {
        super(clsInfo);
    }

    @Override
    protected void releaseResources() {
        targetExceptionClassNames = null;
        super.releaseResources();
    }

    public CatchMethodMetaInfo exceptionClasses(List<String> list) {
        targetExceptionClassNames = C.newList(list);
        return this;
    }

    public List<String> exceptionClasses() {
        if (null == paramTypeAnalyzed) {
            synchronized (this) {
                if (null == paramTypeAnalyzed) {
                    paramTypeAnalyzed = true;
                    analyzeParamTypes();
                }
            }
        }
        return targetExceptionClassNames;
    }

    private void analyzeParamTypes() {
        App app = Act.app();
        List<String> paramExceptions = new ArrayList<>();
        for (int i = 0, n = paramCount(); i < n; ++i) {
            HandlerParamMetaInfo param = param(i);
            String className = param.type().getClassName();
            if (className.endsWith("Exception")) {
                paramExceptions.add(className);
            } else {
                Class<?> cls = app.classForName(className);
                if (Throwable.class.isAssignableFrom(cls)) {
                    paramExceptions.add(className);
                }
            }
        }
        if (!paramExceptions.isEmpty()) {
            if (1 == targetExceptionClassNames.size() && Throwable.class.getName().equals(targetExceptionClassNames.get(0))) {
                // Since we have param declared exception let's remove the default
                // Throwable from exception type match list
                targetExceptionClassNames = paramExceptions;
            } else {
                targetExceptionClassNames.addAll(paramExceptions);
            }
        }
    }


    @Override
    public String toString() {
        return toStrBuffer(S.newBuffer()).toString();
    }

    @Override
    protected S.Buffer toStrBuffer(S.Buffer sb) {
        StringBuilder prependix = S.builder("catch").append(targetExceptionClassNames).append(" ");
        return super.toStrBuffer(sb).prepend(prependix);
    }

    @Override
    protected InterceptorMethodMetaInfo doExtend(ControllerClassMetaInfo clsInfo) {
        return new CatchMethodMetaInfo(this, clsInfo);
    }
}
