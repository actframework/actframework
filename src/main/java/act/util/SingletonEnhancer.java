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

import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import org.osgl.$;
import org.osgl.util.S;

import javax.inject.Singleton;

/**
 * If a certain non-abstract/public class extends {@link SingletonBase} ensure the class
 * has {@link javax.inject.Singleton} annotation, and generate {@link SingletonBase#instance()}
 * implementation
 */
@SuppressWarnings("unused")
public class SingletonEnhancer extends AppByteCodeEnhancer<SingletonEnhancer> {

    private boolean shouldEnhance = false;
    private boolean shouldAddAnnotation = true;
    private String typeName;

    public SingletonEnhancer() {
        super($.F.<String>yes());
    }

    @Override
    protected Class<SingletonEnhancer> subClass() {
        return SingletonEnhancer.class;
    }

    @Override
    protected void reset() {
        this.shouldAddAnnotation = true;
        this.shouldEnhance = false;
        this.typeName = null;
        super.reset();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.typeName = name;
        if (SingletonBase.class.getName().equals(Type.getObjectType(superName).getClassName())) {
            if (isAbstract(access)) {
                logger.warn("SingletonBase sub class is abstract: %s", name);
                return;
            } else if (!isPublic(access)) {
                logger.warn("SingletonBase sub class is not public: %s", name);
                return;
            }
            shouldEnhance = true;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        Type type = Type.getType(desc);
        if (Singleton.class.getName().equals(type.getClassName())) {
            shouldAddAnnotation = false;
            shouldEnhance = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitEnd() {
        if (shouldEnhance) {
            addAnnotationIfNeeded();
            addInstanceMethod();
        }
        super.visitEnd();
    }

    private void addAnnotationIfNeeded() {
        if (shouldAddAnnotation) {
            AnnotationVisitor av = super.visitAnnotation(Type.getType(Singleton.class).getDescriptor(), true);
            av.visitEnd();
        }
    }

    private void addInstanceMethod() {
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "instance", "()Ljava/lang/Object;", "<T:Ljava/lang/Object;>()TT;", null);
        mv.visitCode();
        mv.visitMethodInsn(INVOKESTATIC, "act/app/App", "instance", "()Lact/app/App;", false);
        mv.visitLdcInsn(Type.getType(instanceTypeDesc()));
        mv.visitMethodInsn(INVOKEVIRTUAL, "act/app/App", "singleton", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 0);
        mv.visitEnd();
    }

    private String instanceMethodReturnTypeDesc() {
        return S.fmt("()L%s;", typeName);
    }

    private String instanceTypeDesc() {
        return S.fmt("L%s;", typeName);
    }

}
