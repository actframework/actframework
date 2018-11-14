package act.inject.genie;

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
import act.app.AppByteCodeScannerBase;
import act.app.event.SysEventId;
import act.util.ByteCodeVisitor;

/**
 * Find all classes that ends with `Module`, try to register it as
 * Genie module
 */
public class GenieModuleScanner extends AppByteCodeScannerBase {

    private boolean shouldRegister;

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new ByteCodeVisitor() {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                shouldRegister = isPublic(access);
            }
        };
    }

    @Override
    public void scanFinished(final String className) {
        if (shouldRegister) {
            final App app = app();
            app.jobManager().on(SysEventId.DEPENDENCY_INJECTOR_INITIALIZED, "GenieModuleScanner:addModuleClass:" + className, new Runnable() {
                @Override
                public void run() {
                    GenieInjector.addModuleClass(app.classForName(className));
                }
            });
        }
    }

    @Override
    protected boolean shouldScan(final String className) {
        return className.endsWith("Module");
    }
}
