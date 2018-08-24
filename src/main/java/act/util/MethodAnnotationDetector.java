package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;

public class MethodAnnotationDetector extends AppByteCodeScannerBase {

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        final ClassInfoRepository classInfoRepository = App.instance().classLoader().classInfoRepository();
        return new ByteCodeVisitor() {

            private String classInternalName;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                classInternalName = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (!AsmTypes.isPublic(access)) {
                    return mv;
                }
                return new MethodVisitor(ASM5, mv) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        classInfoRepository.registerMethodAnnotationLookup(desc, classInternalName, name, desc);
                        return super.visitAnnotation(desc, visible);
                    }
                };
            }
        };
    }

    @Override
    public void scanFinished(String className) {
    }
}
