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
import act.asm.*;
import act.sys.Env;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;

/**
 * Scan `@Env.Mode`, `@Env.Profile`, `@Env.Group`
 */
public class EnvAnnotationVisitor extends AnnotationVisitor implements Opcodes {

    public static final String DESC_PROFILE = Type.getType(Env.Profile.class).getDescriptor();
    public static final String DESC_REQUIRE_PROFILE = Type.getType(Env.RequireProfile.class).getDescriptor();
    public static final String DESC_MODE = Type.getType(Env.Mode.class).getDescriptor();
    public static final String DESC_REQUIRE_MODE = Type.getType(Env.RequireMode.class).getDescriptor();
    public static final String DESC_GROUP = Type.getType(Env.Group.class).getDescriptor();
    public static final String DESC_REQUIRE_GROUP = Type.getType(Env.RequireGroup.class).getDescriptor();

    private List<String> arrayValue = new ArrayList<>();
    private boolean matched = true;
    private boolean except = false;
    private String desc;

    public EnvAnnotationVisitor(AnnotationVisitor annotationVisitor, String descriptor) {
        super(ASM5, annotationVisitor);
        this.desc = descriptor;
    }

    @Override
    public void visit(String name, Object value) {
        if ("value".equals(name)) {
            String s = S.string(value);
            if (S.eq(desc, DESC_REQUIRE_MODE) || S.eq(desc, DESC_MODE)) {
                matched = Env.modeMatches(s);
            }
        } else if ("except".equals(name)) {
            except = (Boolean) value;
        }
        super.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor av = super.visitArray(name);
        if ("value".equals(name)) {
            return new AnnotationVisitor(ASM5, av) {
                @Override
                public void visit(String name, Object value) {
                    arrayValue.add(S.string(value));
                    super.visit(name, value);
                }
            };
        }
        return av;
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
        int arrayValueSize = arrayValue.size();
        if (arrayValueSize > 0) {
            if (S.eq(desc, DESC_REQUIRE_PROFILE) || S.eq(desc, DESC_PROFILE)) {
                matched = Env.profileMatches(arrayValue.toArray(new String[arrayValueSize]));
            } else if (S.eq(desc, DESC_REQUIRE_GROUP) || S.eq(desc, DESC_GROUP)) {
                matched = Env.groupMatches(arrayValue.toArray(new String[arrayValueSize]));
            }
        }
        matched = except ^ matched;
        super.visitEnd();
    }

    public boolean matched() {
        return matched;
    }

}
