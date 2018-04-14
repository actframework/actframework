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
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import org.osgl.inject.Module;

import java.util.HashSet;
import java.util.Set;

/**
 * The `GenieFactoryFinder` find classes that contains `@org.osgl.genie.annotation.Provides`
 * annotated factory methods
 */
public class GenieFactoryFinder extends AppByteCodeScannerBase {

    private static Set<String> factories;

    private boolean isFactory;

    private boolean isModule;

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new ByteCodeVisitor() {

            private boolean isPublic;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                isPublic = AsmTypes.isPublic(access);
                isModule = Module.class.getName().equals(Type.getObjectType(superName).getClassName());
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return !isPublic || isModule || isFactory ? mv : new MethodVisitor(ASM5, mv) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        Type annoType = Type.getType(desc);
                        if (AsmTypes.PROVIDES.asmType().equals(annoType)) {
                            isFactory = true;
                        }
                        return super.visitAnnotation(desc, visible);
                    }
                };
            }
        };
    }

    @Override
    public void scanFinished(String className) {
        if (isFactory) {
            factories.add(className);
        }
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    public static void classInit(App app) {
        factories = app.createSet();
    }

    public static void testClassInit() {
        factories = new HashSet<>();
    }

    static Set<String> factories() {
        return factories;
    }
}
