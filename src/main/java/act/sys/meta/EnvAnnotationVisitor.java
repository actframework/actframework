package act.sys.meta;

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
import act.asm.AnnotationVisitor;
import act.asm.Opcodes;
import act.sys.Env;
import org.osgl.util.S;

import java.lang.annotation.Annotation;

/**
 * Scan `@Env.Mode`, `@Env.Profile`, `@Env.Group`
 */
public class EnvAnnotationVisitor extends AnnotationVisitor implements Opcodes {

    private boolean matched = true;
    private boolean unless = false;
    private Class<? extends Annotation> type;

    public EnvAnnotationVisitor(AnnotationVisitor annotationVisitor, Class<? extends Annotation> c) {
        super(ASM5, annotationVisitor);
        this.type = c;
    }

    @Override
    public void visit(String name, Object value) {
        if ("value".equals(name)) {
            String s = S.string(value);
            if (type == Env.Profile.class) {
                matched = Env.profileMatches(s);
            } else if (type == Env.Group.class) {
                matched = Env.groupMatches(s);
            }
        } else if ("unless".equals(name)) {
            unless = (Boolean) value;
        }
        super.visit(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        if ("value".equals(name) && desc.contains("Mode")) {
            Act.Mode mode = Act.Mode.valueOf(value);
            if (!Env.modeMatches(mode)) {
                matched = false;
            }
        }
        super.visitEnum(name, desc, value);
    }

    @Override
    public void visitEnd() {
        matched = unless ^ matched;
        super.visitEnd();
    }

    public boolean matched() {
        return matched;
    }

}
