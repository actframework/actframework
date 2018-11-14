package act.view.rythm;

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
import act.app.AppByteCodeScannerBase;
import act.app.event.SysEventId;
import act.asm.*;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import org.osgl.util.S;
import org.rythmengine.extension.Transformer;

/**
 * Search for method that has {@link org.rythmengine.extension.Transformer} annotation.
 * Register the class if such method found
 */
public class RythmTransformerScanner extends AppByteCodeScannerBase {

    private static final String TRANSFORMER = Transformer.class.getName();

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new Visitor();
    }

    @Override
    public void scanFinished(String className) {
    }

    private class Visitor extends ByteCodeVisitor {

        boolean found;
        String className;

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            if (!AsmTypes.isPublic(access)) {
                return;
            }
            className = Type.getObjectType(name).getClassName();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!found) {
                String className = Type.getType(desc).getClassName();
                if (S.eq(TRANSFORMER, className)) {
                    found = true;
                }
            }
            return super.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (found) {
                return mv;
            }
            return new MethodVisitor(ASM5, mv) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (!found) {
                        String className = Type.getType(desc).getClassName();
                        if (S.eq(TRANSFORMER, className)) {
                            found = true;
                        }
                    }
                    return super.visitAnnotation(desc, visible);
                }
            };
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            if (found) {
                final App app = app();
                app().jobManager().on(SysEventId.PRE_START, "RythmTransformerScanner:registerTransformer:" + className, new Runnable() {
                    @Override
                    public void run() {
                        RythmView rythmView = (RythmView) Act.viewManager().view(RythmView.ID);
                        rythmView.registerTransformer(app(), app.classForName(className));
                    }
                });
            }
        }
    }
}
