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
import act.asm.ClassWriter;
import act.asm.Type;
import act.plugin.Extends;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public abstract class ClassDetector extends ByteCodeVisitor {

    protected static final Logger logger = L.get(ClassDetector.class);

    private String className;

    public abstract boolean found();

    public String className() {
        return className;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = Type.getObjectType(name).getClassName();
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private static class FilteredClassDetector extends ClassDetector {
        private final ClassFilter filter;
        private boolean found = false;
        private boolean skip = false;

        FilteredClassDetector(ClassFilter filter) {
            E.NPE(filter);
            this.filter = filter;
        }

        @Override
        public int hashCode() {
            return $.hc(filter, FilteredClassDetector.class);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof FilteredClassDetector) {
                FilteredClassDetector that = (FilteredClassDetector)obj;
                return $.eq(that.filter, this.filter);
            }
            return false;
        }

        @Override
        public boolean found() {
            return found;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            if (filter.noAbstract() && ((access & ACC_ABSTRACT) != 0 || (access & ACC_INTERFACE) != 0)) {
                skip = true;
                return;
            }
            if (filter.publicOnly() && (access & ACC_PUBLIC) != 1) {
                skip = true;
                return;
            }
            Class<?> superType = filter.superType();
            if (null == superType) {
                return; // we will check annotation type anyway
            }
            final String expected = superType.getName();
            if (checkName(expected, superName)) {
                found = true;
                return;
            }
            int len = interfaces.length;
            for (int i = 0; i < len; ++i) {
                String s = interfaces[i];
                if (checkName(expected, s)) {
                    found = true;
                    return;
                }
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (found || skip) {
                return av;
            }
            if (isExtendsAnnotation(desc)) {
                Class<?> superType = filter.superType();
                if (null != superType) {
                    return new ExtendedAnnotationVisitor(av, superType.getName());
                } else {
                    return av;
                }
            }
            Class<? extends Annotation> annoType = filter.annotationType();
            if (null == annoType) {
                return av;
            }
            if (isAnnotation(annoType, desc)) {
                found = true;
            }
            return av;
        }

        private boolean checkName(String expected, String found) {
            Type type = Type.getObjectType(found);
            return type.getClassName().equals(expected);
        }

        private final class ExtendedAnnotationVisitor extends AnnotationVisitor {

            private String expected;

            public ExtendedAnnotationVisitor(AnnotationVisitor av, String expected) {
                super(ASM5, av);
                this.expected = expected;
            }

            @Override
            public void visit(String name, Object value) {
                found = found || "value".equals(name) && (value instanceof Type) && ((Type) value).getClassName().equals(expected);
                super.visit(name, value);
            }
        }
    }

    private static boolean isExtendsAnnotation(String desc) {
        return isAnnotation(Extends.class, desc);
    }

    private static boolean isAnnotation(Class<? extends Annotation> annoType, String desc) {
        return S.eq(annoType.getName(), Type.getType(desc).getClassName());
    }

    public static ClassDetector chain(ClassWriter cw, ClassFilter... filters) {
        return (ClassDetector) chain(cw, of(filters));
    }

    public static ClassDetector of(final ClassFilter... filters) {
        E.illegalArgumentIf(filters.length == 0);
        if (filters.length == 1) {
            return new FilteredClassDetector(filters[0]);
        }
        return new ClassDetector() {
            C.List<ClassDetector> detectors = C.listOf(filters).map(new $.F1<ClassFilter, ClassDetector>() {
                @Override
                public ClassDetector apply(ClassFilter classFilter) throws NotAppliedException, $.Break {
                    return new FilteredClassDetector(classFilter);
                }
            });
            private List<ClassDetector> matches = new ArrayList<>();

            @Override
            public int hashCode() {
                return $.hc(detectors);
            }

            @Override
            public boolean equals(Object obj) {
                return obj == this;
            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                for (ClassDetector detector : detectors) {
                    detector.visit(version, access, name, signature, superName, interfaces);
                    if (detector.found()) {
                        matches.add(detector);
                    }
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (matches.size() == detectors.size() || !isExtendsAnnotation(desc)) {
                    return av;
                }
                final C.List<ClassDetector> unmatched = detectors.without(matches);
                return new AnnotationVisitor(ASM5, av) {
                    @Override
                    public void visit(String name, Object value) {
                        for (ClassDetector detector : unmatched) {
                            AnnotationVisitor av0 = detector.visitAnnotation(desc, visible);
                            av0.visit(name, value);
                            if (detector.found()) {
                                matches.add(detector);
                            }
                        }
                        super.visit(name, value);
                    }
                };
            }

            @Override
            public boolean found() {
                return !matches.isEmpty();
            }
        };
    }

}
