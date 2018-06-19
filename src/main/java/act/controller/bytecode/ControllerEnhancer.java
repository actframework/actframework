package act.controller.bytecode;

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
import act.asm.MethodVisitor;
import act.asm.Type;
import act.controller.meta.ControllerClassMetaInfo;
import act.controller.meta.ControllerClassMetaInfoHolder;
import act.controller.meta.HandlerMethodMetaInfo;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;

/**
 * Enhance controllers (classes with either request handler method or
 * interceptor methods)
 */
public class ControllerEnhancer extends AppByteCodeEnhancer<ControllerEnhancer> {
    private ControllerClassMetaInfoHolder classInfoHolder;
    private String className;

    public ControllerEnhancer() {
    }

    public ControllerEnhancer(ClassVisitor cv, ControllerClassMetaInfoHolder infoHolder) {
        super(_F.isController(infoHolder), cv);
        this.classInfoHolder = infoHolder;
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        this.classInfoHolder = app.classLoader();
        return super.app(app);
    }

    @Override
    protected Class<ControllerEnhancer> subClass() {
        return ControllerEnhancer.class;
    }

    @Override
    protected void reset() {
        this.className = null;
        super.reset();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = Type.getObjectType(name).getClassName();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        HandlerMethodMetaInfo info = methodInfo(name);
        if (null == info) {
            return mv;
        }
        logger.debug(">>>About to enhance handler: %s", name);
        return new HandlerEnhancer(mv, info, access, name, desc, signature, exceptions);
    }

    private HandlerMethodMetaInfo methodInfo(String name) {
        if (!isConstructor(name)) {
            ControllerClassMetaInfo ccInfo = classInfoHolder.controllerClassMetaInfo(className);
            if (null == ccInfo) {
                return null;
            }
            return ccInfo.handler(name);
        } else {
            return null;
        }
    }

    private enum _F {
        ;

        private static final $.Predicate<String> isController(final ControllerClassMetaInfoHolder infoSrc) {
            return new $.Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return infoSrc.controllerClassMetaInfo(s) != null;
                }
            };
        }
    }
}
