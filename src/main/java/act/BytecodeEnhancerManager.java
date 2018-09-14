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

import static act.Destroyable.Util.tryDestroyAll;

import act.app.App;
import act.asm.ClassWriter;
import act.util.*;
import org.osgl.$;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

public class BytecodeEnhancerManager extends LogSupportedDestroyableBase {
    private List<AppByteCodeEnhancer> appEnhancers = new ArrayList<>();
    private List<AsmByteCodeEnhancer> generalEnhancers = new ArrayList<>();

    public BytecodeEnhancerManager() {
    }

    public void register(AsmByteCodeEnhancer enhancer) {
        if (enhancer instanceof AppByteCodeEnhancer) {
            appEnhancers.add((AppByteCodeEnhancer) enhancer);
        } else {
            generalEnhancers.add(enhancer);
        }
    }

    public void register(AppByteCodeEnhancer enhancer) {
        appEnhancers.add(enhancer);
    }

    public void registered() {
        Collections.sort(appEnhancers);
    }

    public ByteCodeVisitor appEnhancer(App app, String className, $.Var<ClassWriter> cw) {
        List<AppByteCodeEnhancer> l = appFilter(app, className);
        return l.isEmpty() ? null : ByteCodeVisitor.chain(cw, l);
    }

    public ByteCodeVisitor generalEnhancer(String className, $.Var<ClassWriter> cw) {
        List<AsmByteCodeEnhancer> l = generalFilter(className);
        return l.isEmpty() ? null : ByteCodeVisitor.chain(cw, l);
    }

    private List<AppByteCodeEnhancer> appFilter(App app, String className) {
        List<AppByteCodeEnhancer> l = new ArrayList<>();
        for (AppByteCodeEnhancer e : appEnhancers) {
            AppByteCodeEnhancer e0 = (AppByteCodeEnhancer) e.clone();
            e0.app(app);
            if (e0.isTargetClass(className)) {
                l.add(e0);
            }
        }
        return l;
    }

    private List<AsmByteCodeEnhancer> generalFilter(String className) {
        List<AsmByteCodeEnhancer> l = new ArrayList<>();
        for (AsmByteCodeEnhancer e : generalEnhancers) {
            AsmByteCodeEnhancer e0 = e.clone();
            if (e0.isTargetClass(className)) {
                l.add(e0.clone());
            }
        }
        return l;
    }

    @Override
    protected void releaseResources() {
        tryDestroyAll(appEnhancers, ApplicationScoped.class);
        appEnhancers.clear();
        tryDestroyAll(generalEnhancers, ApplicationScoped.class);
        generalEnhancers.clear();
    }
}
