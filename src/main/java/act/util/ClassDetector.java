package act.util;

import act.asm.AnnotationVisitor;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import act.asm.ClassWriter;
import act.asm.Type;
import act.plugin.Extends;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;

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

        FilteredClassDetector(ClassFilter filter) {
            E.NPE(filter);
            this.filter = filter;
        }

        @Override
        public int hashCode() {
            return _.hc(filter, FilteredClassDetector.class);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof FilteredClassDetector) {
                FilteredClassDetector that = (FilteredClassDetector)obj;
                return _.eq(that.filter, this.filter);
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
            if (filter.noAbstract() && (access & ACC_ABSTRACT) != 0) {
                return;
            }
            if (filter.publicOnly() && (access & ACC_PUBLIC) != 1) {
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
            if (found) {
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
                super.visit(name, value);
                found = found || "value".equals(name) && (value instanceof Type) && ((Type) value).getClassName().equals(expected);
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
            C.List<ClassDetector> detectors = C.listOf(filters).map(new _.F1<ClassFilter, ClassDetector>() {
                @Override
                public ClassDetector apply(ClassFilter classFilter) throws NotAppliedException, _.Break {
                    return new FilteredClassDetector(classFilter);
                }
            });
            private C.List<ClassDetector> matches = C.newList();

            @Override
            public int hashCode() {
                return _.hc(detectors);
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
