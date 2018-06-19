package act.cli.bytecode;

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
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommanderClassMetaInfo;
import act.cli.meta.CommanderClassMetaInfoHolder;
import act.util.AppByteCodeEnhancer;

import java.util.HashSet;
import java.util.Set;

public class CommanderEnhancer extends AppByteCodeEnhancer<CommanderEnhancer> {

    private String className;
    private CommanderClassMetaInfoHolder infoBase;
    private CommanderClassMetaInfo metaInfo;

    public CommanderEnhancer() {
    }

    @Override
    protected Class<CommanderEnhancer> subClass() {
        return CommanderEnhancer.class;
    }

    @Override
    protected void reset() {
        this.className = null;
        this.metaInfo = null;
        super.reset();
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        this.infoBase = app.classLoader();
        return super.app(app);
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = Type.getObjectType(name).getClassName();
        metaInfo = infoBase.commanderClassMetaInfo(className);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (null == metaInfo || isConstructor(name)) {
            return mv;
        }
        final CommandMethodMetaInfo methodInfo = metaInfo.command(name);
        if (null == methodInfo) {
            return mv;
        }
        if (isPublic(access) && !isConstructor(name)) {
            return new MethodVisitor(ASM5, mv) {
                private Set<Integer> skipNaming = new HashSet<Integer>();
                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                    if ("Ljavax/inject/Named;".equals(desc)) {
                        skipNaming.add(parameter);
                    }
                    return super.visitParameterAnnotation(parameter, desc, visible);
                }

                @Override
                public void visitEnd() {
                    int sz = methodInfo.paramCount();
                    for (int i = 0; i < sz; ++i) {
                        if (!skipNaming.contains(i)) {
                            String name = methodInfo.param(i).name();
                            AnnotationVisitor av = mv.visitParameterAnnotation(i, "Ljavax/inject/Named;", true);
                            av.visit("value", name);
                        }
                    }
                    super.visitEnd();
                }
            };
        }
        return mv;
    }
}
