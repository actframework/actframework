package act.job.bytecode;

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

import act.app.AppByteCodeScannerBase;
import act.app.event.SysEventId;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Opcodes;
import act.asm.Type;
import act.test.FixtureLoader;
import act.job.JobAnnotationProcessor;
import act.job.meta.JobClassMetaInfo;
import act.job.meta.JobClassMetaInfoManager;
import act.job.meta.JobMethodMetaInfo;
import act.sys.Env;
import act.sys.meta.EnvAnnotationVisitor;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Scan class to collect Job class meta info
 */
public class JobByteCodeScanner extends AppByteCodeScannerBase {

    private JobAnnotationProcessor annotationProcessor;
    private JobClassMetaInfo classInfo;
    private volatile JobClassMetaInfoManager classInfoBase;

    @Override
    protected boolean shouldScan(String className) {
        classInfo = new JobClassMetaInfo();
        return true;
    }

    @Override
    protected void onAppSet() {
        annotationProcessor = new JobAnnotationProcessor(app());
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        classInfoBase().registerJobMetaInfo(classInfo);
    }

    private JobClassMetaInfoManager classInfoBase() {
        if (null == classInfoBase) {
            synchronized (this) {
                if (null == classInfoBase) {
                    classInfoBase = app().classLoader().jobClassMetaInfoManager();
                }
            }
        }
        return classInfoBase;
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            classInfo.className(name);
            Type superType = Type.getObjectType(superName);
            classInfo.superType(superType);
            if (isAbstract(access)) {
                classInfo.setAbstract();
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if (!isEligibleMethod(access, name, desc)) {
                return mv;
            }
            return new JobMethodVisitor(mv, access, name, desc, signature, exceptions);
        }

        private boolean isEligibleMethod(int access, String name, String desc) {
            // TODO: analyze parameters
            return isPublic(access);
        }

        private class JobMethodVisitor extends MethodVisitor implements Opcodes {

            private String methodName;
            private int access;
            private boolean requireScan;
            private JobMethodMetaInfo methodInfo;
            private ActionAnnotationVisitor aav;
            private EnvAnnotationVisitor eav;
            private List<String> paramTypes;

            JobMethodVisitor(MethodVisitor mv, int access, String methodName, String desc, String signature, String[] exceptions) {
                super(ASM5, mv);
                this.access = access;
                this.methodName = methodName;
                Type[] arguments = Type.getArgumentTypes(desc);
                paramTypes = new ArrayList<>();
                if (null != arguments) {
                    for (Type type : arguments) {
                        paramTypes.add(type.getClassName());
                    }
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                Type type = Type.getType(desc);
                String className = type.getClassName();
                try {
                    Class<? extends Annotation> c = (Class<? extends Annotation>)Class.forName(className);
                    if (JobClassMetaInfo.isActionAnnotation(c)) {
                        markRequireScan();
                        if (null == methodInfo) {
                            JobMethodMetaInfo tmp = new JobMethodMetaInfo(classInfo, paramTypes);
                            methodInfo = tmp;
                            classInfo.addAction(tmp);
                            this.aav = new ActionAnnotationVisitor(av, c, methodInfo);
                        } else {
                            this.aav.add(c);
                        }
                        return this.aav;
                    } else if (Env.isEnvAnnotation(c)) {
                        this.eav = new EnvAnnotationVisitor(av, desc);
                        return this.eav;
                    }
                } catch (Exception e) {
                    throw E.unexpected(e);
                }
                //markNotTargetClass();
                return av;
            }

            @Override
            public void visitEnd() {
                if (!requireScan()) {
                    super.visitEnd();
                    return;
                }
                JobMethodMetaInfo info = methodInfo;
                info.name(methodName);
                boolean isStatic = AsmTypes.isStatic(access);
                if (isStatic) {
                    info.invokeStaticMethod();
                } else {
                    info.invokeInstanceMethod();
                }

                if (null != aav) {
                    if (null == eav || eav.matched()) {
                        aav.doRegistration();
                    }
                }
                super.visitEnd();
            }

            private void markRequireScan() {
                this.requireScan = true;
            }

            private boolean requireScan() {
                return requireScan;
            }

            private class ActionAnnotationVisitor extends AnnotationVisitor implements Opcodes {

                List<JobAnnoInfo> annoInfos = new ArrayList<>();
                JobAnnoInfo currentInfo;
                JobMethodMetaInfo method;

                public ActionAnnotationVisitor(AnnotationVisitor av, Class<? extends Annotation> c, JobMethodMetaInfo methodMetaInfo) {
                    super(ASM5, av);
                    this.method = methodMetaInfo;
                    this.add(c);
                }

                void add(Class<? extends Annotation> annotationClass) {
                    currentInfo = new JobAnnoInfo(annotationClass);
                    annoInfos.add(currentInfo);
                }

                @Override
                public void visitEnum(String name, String desc, String value) {
                    if (desc.contains("SysEventId")) {
                        this.currentInfo.sysEventId = SysEventId.valueOf(value);
                    }
                    super.visitEnum(name, desc, value);
                }

                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        if (this.currentInfo.annotationType == FixtureLoader.class) {
                            String id = S.string(value);
                            if (S.blank(id)) {
                                id = methodName;
                            }
                            method.id(id);
                        } else {
                            this.currentInfo.value = value.toString();
                        }
                    } else if ("async".equals(name)) {
                        this.currentInfo.async = $.bool(value);
                    } else if ("id".equals(name)) {
                        this.method.id(S.string(value));
                    } else if ("startImmediately".equals(name)) {
                        this.currentInfo.startImmediately = Boolean.parseBoolean(value.toString());
                    } else if ("delayInSeconds".equals(name)) {
                        this.currentInfo.delayInSeconds = Integer.parseInt(value.toString());
                    }
                    super.visit(name, value);
                }

                public void doRegistration() {
                    for (JobAnnoInfo info : annoInfos) {
                        annotationProcessor.register(method, info.annotationType, info);
                    }
                }
            }
        }
    }


}
