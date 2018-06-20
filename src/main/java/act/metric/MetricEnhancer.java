package act.metric;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import act.asm.*;
import act.asm.commons.AdviceAdapter;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;
import org.osgl.util.S;

public class MetricEnhancer extends AppByteCodeEnhancer<MetricEnhancer> {

    private static final String DESC_MEASURE_TIME = Type.getType(MeasureTime.class).getDescriptor();
    private static final String DESC_MEASURE_COUNT = Type.getType(MeasureCount.class).getDescriptor();
    private static final Type TYPE_METRIC = Type.getType(Metric.class);
    private static final Type TYPE_TIMER = Type.getType(Timer.class);

    private String className;
    private MetricMetaInfoRepo repo;

    public MetricEnhancer() {
    }

    // for unit testing
    MetricEnhancer(MetricMetaInfoRepo repo, ClassVisitor cv) {
        super(S.F.startsWith("act.").negate(), cv);
        this.repo = $.requireNotNull(repo);
    }

    public MetricEnhancer(ClassVisitor cv) {
        super(S.F.startsWith("act.").negate(), cv);
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        repo = app.metricMetaInfoRepo();
        return super.app(app);
    }

    @Override
    protected Class<MetricEnhancer> subClass() {
        return MetricEnhancer.class;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = Type.getObjectType(name).getClassName();
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        final String methodName = name;
        return new AdviceAdapter(ASM5, mv, access, name, desc) {
            private String timeLabel;
            private String countLabel;
            private int posMetric;
            private int posTimer;
            private Label startFinally = new Label();
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (DESC_MEASURE_TIME.equals(desc)) {
                    timeLabel = methodName;
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public void visit(String name, Object value) {
                            super.visit(name, value);
                            if ("value".equals(name)) {
                                timeLabel = S.string(value);
                            }
                        }
                    };
                } else if (DESC_MEASURE_COUNT.equals(desc)) {
                    countLabel = methodName;
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public void visit(String name, Object value) {
                            super.visit(name, value);
                            if ("value".equals(name)) {
                                countLabel = S.string(value);
                            }
                        }
                    };
                }
                return av;
            }
            @Override
            public void visitCode() {
                super.visitCode();
                if (null != timeLabel) {
                    mv.visitLabel(startFinally);
                }
            }
            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                if (null != timeLabel) {
                    Label endFinally = new Label();
                    mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
                    mv.visitLabel(endFinally);
                    onFinally(ATHROW);
                    mv.visitInsn(ATHROW);
                    mv.visitMaxs(maxStack, maxLocals);
                } else {
                    super.visitMaxs(maxStack, maxLocals);
                }
            }
            @Override
            protected void onMethodEnter() {
                if (null == timeLabel && null == countLabel) {
                    return;
                }
                posMetric = newLocal(TYPE_METRIC);
                mv.visitMethodInsn(INVOKESTATIC, "act/Act", "metricPlugin", "()Lact/metric/MetricPlugin;", false);
                String context = repo.contextOfClass(className);
                mv.visitLdcInsn("app");
                mv.visitMethodInsn(INVOKEINTERFACE, "act/metric/MetricPlugin", "metric", "(Ljava/lang/String;)Lact/metric/Metric;", true);
                mv.visitVarInsn(ASTORE, posMetric);
                if (null != countLabel && !countLabel.equalsIgnoreCase(timeLabel)) {
                    mv.visitVarInsn(ALOAD, posMetric);
                    mv.visitLdcInsn(MetricMetaInfoRepo.concat(context, countLabel));
                    mv.visitMethodInsn(INVOKEINTERFACE, "act/metric/Metric", "countOnce", "(Ljava/lang/String;)V", true);
                }
                if (null != timeLabel) {
                    posTimer = newLocal(TYPE_TIMER);
                    mv.visitVarInsn(ALOAD, posMetric);
                    mv.visitLdcInsn(MetricMetaInfoRepo.concat(context, timeLabel));
                    mv.visitMethodInsn(INVOKEINTERFACE, "act/metric/Metric", "startTimer", "(Ljava/lang/String;)Lact/metric/Timer;", true);
                    mv.visitVarInsn(ASTORE, posTimer);
                }
            }

            protected final void onMethodExit(int opcode) {
                if (null == timeLabel) {
                    return;
                }
                if (opcode != ATHROW) {
                    onFinally(opcode);
                }
            }
            private void onFinally(int opcode) {
                mv.visitVarInsn(ALOAD, posTimer);
                mv.visitMethodInsn(INVOKEINTERFACE, "act/metric/Timer", "close", "()V", true);
            }
        };
    }
}
