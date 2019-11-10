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
import act.asm.Opcodes;
import act.asm.Type;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneralAnnoInfo {

    public static class EnumInfo {
        private Type type;
        private String value;

        EnumInfo(String desc, String value) {
            this.type = Type.getType(desc);
            this.value = value;
        }

        public Type type() {
            return type;
        }

        public String value() {
            return value;
        }
    }

    private Type type;
    private Map<String, Object> attributes = new HashMap<>();
    private Map<String, List<Object>> listAttributes = new HashMap<>();

    public GeneralAnnoInfo(Type type) {
        E.NPE(type);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public Map<String, Object> attributes() {
        return C.Map(attributes);
    }

    public GeneralAnnoInfo addAnnotation(String name, Type type) {
        GeneralAnnoInfo anno = new GeneralAnnoInfo(type);
        attributes.put(name, anno);
        return anno;
    }

    public void putAttribute(String name, Object val) {
        attributes.put(name, val);
    }

    public void putListAttribute(String name, Object val) {
        List<Object> vals = listAttributes.get(name);
        if (null == vals) {
            vals = C.newList(val);
            listAttributes.put(name, vals);
        } else {
            vals.add(val);
        }
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public List<Object> getListAttributes(String name) {
        return listAttributes.get(name);
    }

    @Override
    public int hashCode() {
        return $.hc(type, attributes, listAttributes);
    }

    @Override
    public String toString() {
        C.List<String> keys = C.newList(attributes.keySet()).append(listAttributes.keySet()).sorted();
        S.Buffer sb = S.newBuffer("@").append(type.getClassName()).append("(");
        for (String key: keys) {
            Object v = attributes.get(key);
            if (null == v) {
                v = listAttributes.get(v);
            }
            sb.append(key).append("=").append(v).append(", ");
        }
        if (!keys.isEmpty()) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
        return sb.toString();
    }

    public <T extends Annotation> T toAnnotation() {
        return AnnotationInvocationHandler.proxy(this);
    }

    public static class Visitor extends AnnotationVisitor implements Opcodes {
        private GeneralAnnoInfo anno;

        public Visitor(AnnotationVisitor av, GeneralAnnoInfo anno) {
            super(ASM5, av);
            E.NPE(anno);
            this.anno = anno;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            AnnotationVisitor av = super.visitAnnotation(name, desc);
            GeneralAnnoInfo annoAnno = anno.addAnnotation(name, Type.getType(desc));
            return av;
        }

        @Override
        public void visit(String name, Object value) {
            anno.putAttribute(name, value);
            super.visit(name, value);
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            anno.putAttribute(name, new EnumInfo(desc, value));
            super.visitEnum(name, desc, value);
        }

        @Override
        public AnnotationVisitor visitArray(final String name) {
            AnnotationVisitor av = super.visitArray(name);
            return new AnnotationVisitor(ASM5, av) {
                @Override
                public void visitEnum(String ignore, String desc, String value) {
                    anno.putListAttribute(name, new EnumInfo(desc, value));
                    super.visitEnum(ignore, desc, value);
                }

                @Override
                public void visit(String ignore, Object value) {
                    anno.putListAttribute(name, value);
                    super.visit(ignore, value);
                }
            };
        }
    }

    public static class AnnotationInvocationHandler<T extends Annotation> implements Annotation, InvocationHandler, Serializable {
        private static final long serialVersionUID = 8157022630814320170L;
        private final GeneralAnnoInfo annoInfo;
        private final Class<T> annotationType;
        private final int hashCode;
        private final Map<String, Object> values;

        AnnotationInvocationHandler(GeneralAnnoInfo annoInfo, ClassLoader classLoader) {
            this.annotationType = $.classForName(annoInfo.type().getClassName(), classLoader);
            this.annoInfo = annoInfo;
            this.hashCode = annoInfo.hashCode();
            this.values = retrieveAnnotationValues(annoInfo, annotationType);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String key = method.getName();
            Object val = values.get(key);
            return null == val ? method.invoke(this, args) : val;
        }

        private <T> Object toArray(List<Object> list) {
            Class<T> c = (Class<T>)list.get(0).getClass();
            int size = list.size();
            if (c == String.class) {
                return list.toArray(new String[size]);
            } else if (c == Class.class) {
                return list.toArray(new Class[size]);
            } else if (c == Boolean.class) {
                return $.asPrimitive(list.toArray(new Boolean[size]));
            } else if (c == Byte.class) {
                return $.asPrimitive(list.toArray(new Byte[size]));
            } else if (c == Short.class) {
                return $.asPrimitive(list.toArray(new Short[size]));
            } else if (c == Character.class) {
                return $.asPrimitive(list.toArray(new Character[size]));
            } else if (c == Integer.class) {
                return $.asPrimitive(list.toArray(new Integer[size]));
            } else if (c == Float.class) {
                return $.asPrimitive(list.toArray(new Float[size]));
            } else if (c == Long.class) {
                return $.asPrimitive(list.toArray(new Long[size]));
            } else if (c == Double.class) {
                return $.asPrimitive(list.toArray(new Double[size]));
            } else {
                return list.toArray((T[])Array.newInstance(c, size));
            }
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return annotationType;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!annotationType.isInstance(obj)) {
                return false;
            }

            Annotation other = annotationType.cast(obj);

            //compare annotation member values
            for (Map.Entry<String, Object> member : annoInfo.attributes.entrySet()) {
                Object value = member.getValue();
                Object otherValue = getAnnotationMemberValue(other, member.getKey());
                if (!$.eq2(value, otherValue)) {
                    return false;
                }
            }
            for (Map.Entry<String, List<Object>> member : annoInfo.listAttributes.entrySet()) {
                Object value = toArray(member.getValue());
                Object otherValue = getAnnotationMemberValue(other, member.getKey());
                if (!$.eq2(value, otherValue)) {
                    return false;
                }
            }

            return true;
        }


        /**
         * Calculates the hash code of this annotation proxy as described in
         * {@link Annotation#hashCode()}.
         *
         * @return The hash code of this proxy.
         * @see Annotation#hashCode()
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return annoInfo.toString();
        }

        private Map<String, Object> retrieveAnnotationValues(GeneralAnnoInfo info, Class<T> type) {
            Map<String, Object> bag = new HashMap<>();
            for (String key : info.attributes.keySet()) {
                bag.put(key, info.getAttribute(key));
            }
            for (String key : info.listAttributes.keySet()) {
                bag.put(key, toArray(info.getListAttributes(key)));
            }
            Method[] ma = type.getDeclaredMethods();
            for (Method m: ma) {
                String mn = m.getName();
                if (!bag.containsKey(mn)) {
                    bag.put(mn, m.getDefaultValue());
                }
            }
            return bag;
        }


        private Object getAnnotationMemberValue(Annotation annotation, String name) {
            return $.invokeVirtual(annotation, name);
        }

        public static <T extends Annotation> T proxy(GeneralAnnoInfo info) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return proxy(info, cl);
        }

        public static <T extends Annotation> T proxy(GeneralAnnoInfo info, ClassLoader cl) {
            AnnotationInvocationHandler handler = new AnnotationInvocationHandler(info, cl);
            return (T) Proxy.newProxyInstance(cl, new Class[]{handler.annotationType()}, handler);
        }

    }
}
