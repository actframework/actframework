package act.inject.param;

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

import act.asm.*;
import act.inject.param.JsonDtoClassManager.DynamicClassLoader;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;

class JsonDtoClassGenerator implements Opcodes {


    private static final String JSON_DTO_CLASS = "act/inject/param/JsonDto";

    private String className;
    private List<BeanSpec> beanSpecs;
    private DynamicClassLoader dynamicClassLoader;
    private ClassWriter cw;
    private MethodVisitor mv;
    private Map<String, Class> typeParamLookup;

    JsonDtoClassGenerator(String name, List<BeanSpec> list, DynamicClassLoader classLoader, Map<String, Class> typeParamLookup) {
        this.className = name;
        this.beanSpecs = list;
        this.dynamicClassLoader = classLoader;
        this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        this.typeParamLookup = typeParamLookup;
    }

    Class<? extends JsonDto> generate() {
        return $.cast(dynamicClassLoader.defineClass(className, generateByteCode()));
    }

    byte[] generateByteCode() {
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, JSON_DTO_CLASS, null);
        generateConstructor();
        generateSetters();
        return cw.toByteArray();
    }

    private void generateConstructor() {
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "act/inject/param/JsonDto", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void generateSetters() {
        for (BeanSpec beanSpec : beanSpecs) {
            generateSetter(beanSpec);
        }
    }

    private void generateSetter(BeanSpec beanSpec) {
        String setterName = setterName(beanSpec);
        if (setterName.contains(".")) {
            return;
        }
        mv = cw.visitMethod(ACC_PUBLIC, setterName, setterDescriptor(beanSpec), setterSignature(beanSpec), null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(beanSpec.name());
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "set", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitInsn(RETURN);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", S.fmt("L%s;", className), null, l0, l2, 0);
        mv.visitLocalVariable("v", classDesc(beanSpec.rawType()), null, l0, l2, 1);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    private static String setterDescriptor(BeanSpec spec) {
        return S.fmt("(%s)V", classDesc(spec.rawType()));
    }

    private String setterSignature(BeanSpec spec) {
        return S.fmt("(%s)V", typeDesc(spec));
    }

    private String typeDesc(BeanSpec spec) {
        String root = classDesc(spec.rawType());
        List<java.lang.reflect.Type> typeParams = spec.typeParams();
        if (typeParams.isEmpty()) {
            return root;
        }
        S.Buffer sb = S.newBuffer("<");
        for (java.lang.reflect.Type type : typeParams) {
            BeanSpec specx = BeanSpec.of(type, null, spec.injector(), typeParamLookup);
            sb.append(typeDesc(specx));
        }
        sb.append(">");
        FastStr str = FastStr.of(root);
        str = str.take(str.length() - 1).append(sb.toString()).append(";");
        return str.toString();
    }

    private static String classDesc(Class c) {
        if (c.isPrimitive()) {
            c = $.wrapperClassOf(c);
        }
        return Type.getDescriptor(c);
    }

    private static String setterName(BeanSpec beanSpec) {
        return S.concat("set", S.capFirst(beanSpec.name()));
    }
}
