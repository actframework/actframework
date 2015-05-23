package org.osgl.oms.util;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.oms.asm.AnnotationVisitor;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.asm.Type;
import org.osgl.oms.plugin.Extends;
import org.osgl.util.C;
import org.osgl.util.E;

public abstract class ClassDetector extends ByteCodeVisitor {

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

        FilteredClassDetector(ClassFilter filter) {
            E.NPE(filter);
            this.filter = filter;
        }

        @Override
        public boolean found() {
            return found;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            final String expected = filter.superType().getName();
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
            if (found || !isExtendsAnnotation(desc)) {
                return av;
            }
            return new ExtendsAnnotationVisitor(av, filter.superType().getName());
        }

        private boolean checkName(String expected, String found) {
            Type type = Type.getObjectType(found);
            return type.getClassName().equals(expected);
        }

        private final class ExtendsAnnotationVisitor extends AnnotationVisitor {

            private String expected;

            public ExtendsAnnotationVisitor(AnnotationVisitor av, String expected) {
                super(ASM5, av);
                this.expected = expected;
            }

            @Override
            public void visit(String name, Object value) {
                super.visit(name, value);
                found = found || "value".equals(name) && (value instanceof Type) && ((Type) value).getClassName().equals(expected);
            }
        }
    }

    private static boolean isExtendsAnnotation(String desc) {
        return Extends.class.getName().equals(Type.getType(desc).getClassName());
    }

    public static ClassDetector chain(ClassWriter cw, ClassFilter... filters) {
        return (ClassDetector) ByteCodeVisitor.chain(cw, of(filters));
    }

    public static ClassDetector of(final ClassFilter... filters) {
        E.illegalArgumentIf(filters.length == 0);
        if (filters.length == 1) {
            return new FilteredClassDetector(filters[0]);
        }
        return new ClassDetector() {
            C.List<ClassDetector> detectors = C.listOf(filters).map(new _.F1<ClassFilter, ClassDetector>() {
                @Override
                public ClassDetector apply(ClassFilter classFilter) throws NotAppliedException, _.Break {
                    return new FilteredClassDetector(classFilter);
                }
            });
            private C.List<ClassDetector> matches = C.newList();

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
                        super.visit(name, value);
                        for (ClassDetector detector : unmatched) {
                            AnnotationVisitor av0 = detector.visitAnnotation(desc, visible);
                            av0.visit(name, value);
                            if (detector.found()) {
                                matches.add(detector);
                            }
                        }
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
