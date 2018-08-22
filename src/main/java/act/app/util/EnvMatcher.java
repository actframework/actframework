package act.app.util;

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
import act.exception.EnvNotMatchException;
import act.sys.Env;
import act.util.ByteCodeVisitor;
import org.osgl.util.E;
import org.rythmengine.utils.S;

import java.util.ArrayList;
import java.util.List;

/**
 * Detect if a class has `Env` annotations and check if
 * the class's `Env` annotation matches current running environment
 */
public class EnvMatcher extends ByteCodeVisitor {

    public EnvMatcher() {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (desc.startsWith("Lact/sys/Env$")) {
            return new EnvAnnotationVisitor(av, desc);
        }
        return av;
    }

    private static class EnvAnnotationVisitor extends AnnotationVisitor {

        enum Type {
            Profile() {
                @Override
                boolean matches(EnvAnnotationVisitor visitor) {
                    return Env.profileMatches(visitor.arrayValue.toArray(new String[visitor.arrayValue.size()]), visitor.except);
                }
            }, Group() {
                @Override
                boolean matches(EnvAnnotationVisitor visitor) {
                    return Env.groupMatches(visitor.arrayValue.toArray(new String[visitor.arrayValue.size()]), visitor.except);
                }
            }, Mode() {
                @Override
                boolean matches(EnvAnnotationVisitor visitor) {
                    return Env.modeMatches(Act.Mode.valueOf(visitor.value), visitor.except);
                }
            };

            abstract boolean matches(EnvAnnotationVisitor visitor);
        }

        private Type type;
        private List<String> arrayValue = new ArrayList<>();
        private String value;
        private boolean except;

        public EnvAnnotationVisitor(AnnotationVisitor av, String desc) {
            super(ASM5, av);
            initType(desc);
        }

        @Override
        public void visit(String name, Object value) {
            if ("value".equals(name)) {
                this.value = value.toString();
            } else if ("except".equals(name)) {
                this.except = Boolean.parseBoolean(value.toString());
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
            if ("value".equals(name)) {
                this.value = value;
            }
            super.visitEnum(name, desc, value);
        }

        private void initType(String desc) {
            if (desc.contains("Profile")) {
                type = Type.Profile;
            } else if (desc.contains("Mode")) {
                type = Type.Mode;
            } else if (desc.contains("Group")) {
                type = Type.Group;
            } else {
                throw E.unexpected("Unknown Env annotation: %s", desc);
            }
        }

        @Override
        public void visitEnd() {
            if (!matches()) {
                throw new EnvNotMatchException();
            }
            super.visitEnd();
        }

        private boolean matches() {
            return type.matches(this);
        }
    }
}
