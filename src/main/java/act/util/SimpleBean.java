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

import act.Act;
import act.Destroyable;
import act.annotations.Alias;
import act.app.App;
import act.app.AppByteCodeScannerBase;
import act.app.AppClassLoader;
import act.app.event.SysEventId;
import act.asm.*;
import act.internal.password.PasswordMetaInfo;
import act.meta.ClassMetaInfoManager;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Modifier;
import java.util.*;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * ActFramework will do the following byte code enhancement to classes that are
 * instance of `SimpleBean:
 * <p>
 * 1. Create default constructor if not provided (usually for ORM library usage)
 * 2. Create getter/setter for public non-static fields
 * 3. Convert all public fields access into getter/setter calls
 */
public interface SimpleBean {

    /**
     * Keep track of the class info needed by simple bean enhancement logic
     */
    @ApplicationScoped
    class MetaInfo extends DestroyableBase {
        // the class name
        private String className;
        // keep public non-static fields
        private Map<String, $.T2<String, String>> publicFields;
        // keep track @Sensitive fields
        private Set<String> sensitiveFields;
        private Set<String> passwordFields;
        private Map<String, String> fieldAliases;
        private Map<String, String> fieldLabels;

        @Inject
        MetaInfo(String className, Map<String, $.T2<String, String>> publicFields, Map<String, String> aliases, Map<String, String> labels) {
            this.className = className;
            this.publicFields = publicFields;
            this.sensitiveFields = new HashSet<>();
            this.passwordFields = new HashSet<>();
            this.fieldAliases = aliases;
            this.fieldLabels = labels;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public Map<String, $.T2<String, String>> getPublicFields() {
            return C.Map(publicFields);
        }

        public void addSensitiveField(String fieldName) {
            E.illegalArgumentIf(passwordFields.contains(fieldName), "@Sensitive and @Password cannot be used together");
            sensitiveFields.add(fieldName);
        }

        public void addPasswordField(String fieldName) {
            E.illegalArgumentIf(sensitiveFields.contains(fieldName), "@Sensitive and @Password cannot be used together");
            passwordFields.add(fieldName);
        }

        public boolean isSensitive(String fieldName) {
            return sensitiveFields.contains(fieldName);
        }

        public String aliasOf(String fieldName) {
            String s = fieldAliases.get(fieldName);
            return null == s ? fieldName : s;
        }

        public String labelOf(String fieldName) {
            String s = fieldLabels.get(fieldName);
            return null == s ? fieldName : s;
        }

        public boolean isPassword(String fieldName) {
            return passwordFields.contains(fieldName);
        }

        public boolean hasPublicField() {
            return !publicFields.isEmpty();
        }

        @Override
        protected void releaseResources() {
            publicFields.clear();
        }

        static boolean isBoolean(String desc) {
            return "Z".equals(desc) || "java/lang/Boolean".equals(desc);
        }
    }

    @ApplicationScoped
    class MetaInfoManager extends LogSupportedDestroyableBase {
        private static final String INTF_SIMPLE_BEAN = SimpleBean.class.getName();
        private ClassInfoRepository classInfoRepository;
        private Map<String, MetaInfo> registry = new HashMap<>();

        @Inject
        public MetaInfoManager(AppClassLoader classLoader) {
            classInfoRepository = classLoader.classInfoRepository();
        }

        @Override
        protected void releaseResources() {
            Destroyable.Util.tryDestroyAll(registry.values(), ApplicationScoped.class);
            registry.clear();
        }

        public void register(MetaInfo metaInfo) {
            registry.put(metaInfo.getClassName(), metaInfo);
        }

        public boolean isPublicField(String className, String field) {
            MetaInfo metaInfo = get(className);
            return null != metaInfo && metaInfo.publicFields.containsKey(field) && isSimpleBean(className);
        }

        private boolean isSimpleBean(String className) {
            ClassNode node = classInfoRepository.node(className);
            if (null == node) {
                return false;
            }
            return !node.isInterface() && node.hasInterface(INTF_SIMPLE_BEAN);
        }

        public MetaInfo get(String className) {
            return registry.get(className);
        }

        public $.Option<MetaInfo> safeGet(String className) {
            return $.some(get(className));
        }
    }

    class ByteCodeScanner extends AppByteCodeScannerBase {

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        @Override
        public ByteCodeVisitor byteCodeVisitor() {
            return new SimpleBeanByteCodeVisitor();
        }

        @Override
        public void scanFinished(String className) {

        }

        private static class SimpleBeanByteCodeVisitor extends ByteCodeVisitor {

            private static final String RECORD = "java/lang/Record";
            private static final String SIMPLE_BEAN = "act/util/SimpleBean";

            private static final String ALIAS_DESC = Type.getType(Alias.class).getDescriptor();
            private static final String LABEL_DESC = Type.getType(Label.class).getDescriptor();
            private String className;
            private boolean isPublicClass;
            // key: (desc, signature)
            private Map<String, $.T2<String, String>> publicFields = new LinkedHashMap<>();
            private Map<String, String> aliases = new LinkedHashMap<>();
            private Map<String, String> labels = new LinkedHashMap<>();
            private boolean isRecord;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (isPublic(access)) {
                    isPublicClass = true;
                    className = Type.getObjectType(name).getClassName();
                    isRecord = RECORD.equals(superName);
                    if (isRecord) {
                        interfaces = new String[] {SIMPLE_BEAN};
                    }
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, final String name, String desc, String signature, Object value) {
                if (isRecord) access = Modifier.PUBLIC;
                FieldVisitor fv = super.visitField(access, name, desc, signature, value);
                if (isPublicClass && AsmTypes.isPublic(access) && !AsmTypes.isStatic(access)) {
                    publicFields.put(name, $.T2(desc, signature));
                    return new FieldVisitor(ASM5, fv) {
                        private String alias;
                        private String label;
                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            AnnotationVisitor av = super.visitAnnotation(desc, visible);
                            if (ALIAS_DESC.equals(desc)) {
                                return new AnnotationVisitor(ASM5, av) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        alias = S.string(value);
                                    }
                                };
                            } else if (LABEL_DESC.equals(desc)) {
                                return new AnnotationVisitor(ASM5, av) {
                                    @Override
                                    public void visit(String name, Object value) {
                                        label = S.string(value);
                                    }
                                };
                            }
                            return av;
                        }

                        @Override
                        public void visitEnd() {
                            if (null != alias && S.neq(name, alias)) {
                                aliases.put(name, alias);
                                aliases.put(alias, name);
                            }
                            if (null != label && S.neq(name, label)) {
                                labels.put(name, label);
                                labels.put(label, name);
                            }
                            super.visitEnd();
                        }
                    };
                }
                return fv;
            }

            @Override
            public void visitEnd() {
                if (isPublicClass) {
                    Act.app().jobManager().on(SysEventId.APP_CODE_SCANNED, "SimpleBeanByteCodeVisitor:registerToMetaInfoManager:" + className, new Runnable() {
                        @Override
                        public void run() {
                            SimpleBean.MetaInfoManager metaInfoManager = Act.app().classLoader().simpleBeanInfoManager();
                            if (metaInfoManager.isSimpleBean(className)) {
                                MetaInfo metaInfo = new MetaInfo(className, publicFields, aliases, labels);
                                metaInfoManager.register(metaInfo);
                            }
                        }
                    });
                }
                super.visitEnd();
            }
        }
    }

    class ByteCodeEnhancer extends AppByteCodeEnhancer<ByteCodeEnhancer> {

        public static final int PRIORITY = 9;

        private MetaInfoManager metaInfoManager;
        private ClassInfoRepository classInfoRepository;
        private boolean isSimpleBean = false;
        private boolean hasPublicFields = false;
        private Set<String> aliases = new HashSet<>();
        private Map<String, $.T2<String, String>> getters = new LinkedHashMap<>();
        private Map<String, $.T2<String, String>> setters = new LinkedHashMap<>();
        private boolean needDefaultConstructor = false;
        private String classDesc;
        private String superClassDesc;
        private MetaInfo metaInfo;
        private ClassMetaInfoManager<PasswordMetaInfo> passwordMetaInfoManager;
        private PasswordMetaInfo passwordMetaInfo;

        public ByteCodeEnhancer() {
        }

        public ByteCodeEnhancer(ClassVisitor cv) {
            super(S.F.startsWith("act.").negate(), cv);
        }


        @Override
        protected Class<ByteCodeEnhancer> subClass() {
            return ByteCodeEnhancer.class;
        }

        @Override
        public int priority() {
            return PRIORITY;
        }

        @Override
        protected void reset() {
            this.isSimpleBean = false;
            this.hasPublicFields = false;
            this.aliases.clear();
            this.getters.clear();
            this.setters.clear();
            this.needDefaultConstructor = false;
            this.classDesc = null;
            this.superClassDesc = null;
            this.metaInfo = null;
            super.reset();
        }

        @Override
        public AppByteCodeEnhancer app(App app) {
            AppClassLoader classLoader = app.classLoader();
            classInfoRepository = classLoader.classInfoRepository();
            metaInfoManager = classLoader.simpleBeanInfoManager();
            passwordMetaInfoManager = app.classMetaInfoManager(PasswordMetaInfo.class);
            return super.app(app);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            classDesc = name;
            String className = Type.getObjectType(name).getClassName();
            passwordMetaInfo = passwordMetaInfoManager.get(className);
            isSimpleBean = metaInfoManager.isSimpleBean(className);
            if (isSimpleBean) {
                needDefaultConstructor = true;
                MetaInfo metaInfo = metaInfoManager.get(className);
                superClassDesc = superName;
                if (null != metaInfo) {
                    this.metaInfo = metaInfo;
                    Map<String, $.T2<String, String>> publicFields = metaInfo.publicFields;
                    if (!publicFields.isEmpty()) {
                        getters.putAll(publicFields);
                        setters.putAll(publicFields);
                        hasPublicFields = true;
                    } else {
                        hasPublicFields = false;
                    }
                }
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = new PropertyAssignMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), name);
            if (!isSimpleBean) {
                return mv;
            }

            if ("<init>".equals(name) && "()V".equals(desc)) {
                needDefaultConstructor = false;
            }

            if (!hasPublicFields) {
                return mv;
            }
            /*
             * Check if there are getter or setter function already defined for a field
             * Note we don't check the signature and access here intentionally
             */
            String getter = fieldNameFromGetterSetter(name, true);
            if (null != getter) {
                // found getter for the field
                getters.remove(getter);
                //getters.remove(metaInfo.aliasOf(getter));
            }
            String setter = fieldNameFromGetterSetter(name, false);
            if (null != setter) {
                // found setter for the field
                setters.remove(setter);
                ///setters.remove(metaInfo.aliasOf(setter));
            }
            return mv;
        }

        private String fieldNameFromGetterSetter(String methodName, boolean getter) {
            if (methodName.startsWith(getter ? "get" : "set")) {
                String fieldName = methodName.substring(3);
                if (!fieldName.isEmpty()) {
                    char c = fieldName.charAt(0);
                    if (Character.isUpperCase(c)) {
                        return ("" + fieldName.charAt(0)).toLowerCase() + fieldName.substring(1);
                    }
                }
            }
            return getter ? fieldNameFromBooleanGetter(methodName) : null;
        }

        private String fieldNameFromGetterSetter(String methodName) {
            if (methodName.startsWith("get") || methodName.startsWith("set")) {
                String fieldName = methodName.substring(3);
                if (!fieldName.isEmpty()) {
                    char c = fieldName.charAt(0);
                    if (Character.isUpperCase(c)) {
                        return ("" + fieldName.charAt(0)).toLowerCase() + fieldName.substring(1);
                    }
                }
            }
            return fieldNameFromBooleanGetter(methodName);
        }

        private String fieldNameFromBooleanGetter(String methodName) {
            if (methodName.startsWith("is")) {
                String fieldName = methodName.substring(2);
                if (!fieldName.isEmpty()) {
                    char c = fieldName.charAt(0);
                    if (Character.isUpperCase(c)) {
                        return ("" + fieldName.charAt(0)).toLowerCase() + fieldName.substring(1);
                    }
                }
            }
            return null;
        }

        @Override
        public void visitEnd() {
            if (needDefaultConstructor) {
                addDefaultConstructor();
            }
            if (!getters.isEmpty()) {
                addGetters();
            }
            if (!setters.isEmpty()) {
                addSetters();
            }
            super.visitEnd();
        }

        private void addDefaultConstructor() {
            MethodVisitor mv = visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superClassDesc, "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        private void addGetters() {
            for (Map.Entry<String, $.T2<String, String>> field : C.list(getters.entrySet())) {
                addGetter(field);
            }
        }

        private void addSetters() {
            for (Map.Entry<String, $.T2<String, String>> field : C.list(setters.entrySet())) {
                addSetter(field);
            }
        }

        private void addGetter(Map.Entry<String, $.T2<String, String>> field) {
            String name = field.getKey();
            if (metaInfo.isSensitive(name)) {
                return;
            }
            String desc = field.getValue()._1;
            String signature = field.getValue()._2;
            if (null != signature) {
                signature = S.concat("()", signature);
            }

            MethodVisitor mv = visitMethod(ACC_PUBLIC, getterName(name, MetaInfo.isBoolean(desc)), S.concat("()", desc), signature, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, classDesc, name, desc);
            mv.visitInsn(returnCode(desc));
            if (_D == desc.hashCode() || _J == desc.hashCode()) {
                mv.visitMaxs(2, 1);
            } else {
                mv.visitMaxs(1, 1);
            }
            mv.visitEnd();
        }

        private void addSetter(Map.Entry<String, $.T2<String, String>> field) {
            String name = field.getKey();
            if (metaInfo.isSensitive(name) || null != passwordMetaInfo && passwordMetaInfo.isPasswordField(name)) {
                return;
            }
            String desc = field.getValue()._1;
            String signature = field.getValue()._2;

            if (null != signature) {
                signature = S.concat("(", signature, ")V");
            }
            MethodVisitor mv = visitMethod(ACC_PUBLIC, setterName(name), S.concat("(", desc, ")V"), signature, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(loadCode(desc), 1);
            mv.visitFieldInsn(PUTFIELD, classDesc, name, desc);
            mv.visitInsn(RETURN);
            if (_D == desc.hashCode() || _J == desc.hashCode()) {
                mv.visitMaxs(3, 3);
            } else {
                mv.visitMaxs(2, 2);
            }
            mv.visitEnd();
        }

        private String getterName(String fieldName, boolean isBoolean) {
            return (isBoolean ? "is" : "get") + S.capFirst(fieldName);
        }

        private String setterName(String fieldName) {
            return "set" + S.capFirst(fieldName);
        }

        private static final int _I = 'I';
        private static final int _Z = 'Z';
        private static final int _S = 'S';
        private static final int _B = 'B';
        private static final int _C = 'C';

        private static final int _D = 'D';
        private static final int _J = 'J';
        private static final int _F = 'F';


        private int loadCode(String desc) {
            return loadOrReturnCode(desc, true);
        }

        private int returnCode(String desc) {
            return loadOrReturnCode(desc, false);
        }

        private int loadOrReturnCode(String desc, boolean load) {
            switch (desc.hashCode()) {
                case _I:
                case _Z:
                case _S:
                case _B:
                case _C:
                    return load ? ILOAD : IRETURN;
                case _J:
                    return load ? LLOAD : LRETURN;
                case _D:
                    return load ? DLOAD : DRETURN;
                case _F:
                    return load ? FLOAD : FRETURN;
                default:
                    return load ? ALOAD : ARETURN;
            }
        }

        private class PropertyAssignMethodVisitor extends MethodVisitor {
            private String fieldName;

            public PropertyAssignMethodVisitor(MethodVisitor upstream, String name) {
                super(ASM5, upstream);
                this.fieldName = fieldNameFromGetterSetter(name);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (shouldReplaceWithGetterOrSetter(owner, name)) {
                    switch (opcode) {
                        case PUTFIELD:
                            visitMethodInsn(INVOKEVIRTUAL, owner, setterName(name), "(" + desc + ")V");
                            break;
                        case GETFIELD:
                            visitMethodInsn(INVOKEVIRTUAL, owner, getterName(name, MetaInfo.isBoolean(desc)), "()" + desc);
                            break;
                        default:
                            throw new IllegalStateException("visitFieldInsn opcode not supported: " + opcode);
                    }
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
            }

            private boolean shouldReplaceWithGetterOrSetter(String owner, String name) {
                ClassNode node = classInfoRepository.node(owner);
                if (null == node || !node.hasInterface(SimpleBean.class.getName())) {
                    return false;
                }
                String ownerClass = Type.getObjectType(owner).getClassName();
                MetaInfo metaInfo = metaInfoManager.get(ownerClass);
                while (true) {
                    if (null != metaInfo && metaInfo.publicFields.containsKey(name)) {
                        return !insideGetterSetter(owner, name, metaInfo.aliasOf(name));
                    }
                    node = node.parent();
                    if (null == node) {
                        return false;
                    }
                    metaInfo = metaInfoManager.get(node.name());
                }
            }

            private boolean insideGetterSetter(String owner, String name, String alias) {
                return !(null == fieldName || S.neq(classDesc, owner) || (S.neq(fieldName, name) && S.neq(fieldName, alias)));
            }
        }
    }

}
