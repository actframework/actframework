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

import act.app.AppByteCodeScannerBase;
import act.app.event.SysEventId;
import act.asm.AnnotationVisitor;
import act.asm.Type;
import act.event.SysEventListenerBase;

import java.util.EventObject;

public class ClassInfoByteCodeScanner extends AppByteCodeScannerBase {

    private ClassInfoRepository classInfoRepository;

    public ClassInfoByteCodeScanner(ClassInfoRepository classInfoRepository) {
        this.classInfoRepository = classInfoRepository;
    }

    public ClassInfoByteCodeScanner() {
    }

    @Override
    protected void onAppSet() {
        app().eventBus().bind(SysEventId.CLASS_LOADER_INITIALIZED, new SysEventListenerBase("init-class-info-repo") {
            @Override
            public void on(EventObject event) {
                classInfoRepository = app().classLoader().classInfoRepository();
            }
        });
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {

        ClassNode me;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            if (null == superName) {
                return; // ignore java.lang.Object
            }
            String superType = Type.getObjectType(superName).getClassName();
            String myName = Type.getObjectType(name).getClassName();
            me = classInfoRepository.node(myName);
            if (me.modifiers() == 0) {
                // ASM access some how contains non-class modifiers, get rid of them
                access &= ~ACC_SYNCHRONIZED;
                access &= ~ACC_VOLATILE;
                access &= ~ACC_BRIDGE;
                access &= ~ACC_VARARGS;
                access &= ~ACC_TRANSIENT;
                access &= ~ACC_NATIVE;
                access &= ~ACC_STRICT;
                access &= ~ACC_MANDATED;
                me.modifiers(access);
            }
            if (!me.hasInterfaces() && null != interfaces) {
                for (String intf: interfaces) {
                    me.addInterface(intf);
                }
            }
            if (null == me.parent() && !Object.class.getName().equals(superType)) {
                me.parent(superType);
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            String annotationType = Type.getType(desc).getClassName();
            me.annotatedWith(annotationType);
            return super.visitAnnotation(desc, visible);
        }
    }
}
