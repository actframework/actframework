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

import act.app.AppByteCodeScannerBase;
import act.asm.AnnotationVisitor;
import act.asm.Type;
import act.util.ByteCodeVisitor;

public class MetricContextScanner extends AppByteCodeScannerBase {

    private static final String DESC_METRIC_CONTEXT = Type.getType(MetricContext.class).getDescriptor();
    private MetricMetaInfoRepo repo;

    @Override
    protected void onAppSet() {
        repo = app().metricMetaInfoRepo();
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new ByteCodeVisitor() {

            private String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                className = Type.getObjectType(name).getClassName();
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (DESC_METRIC_CONTEXT.equals(desc)) {
                    return new AnnotationVisitor(ASM5, av) {
                        @Override
                        public void visit(String name, Object value) {
                            super.visit(name, value);
                            if ("value".equals(name)) {
                                repo.registerMetricContext(className, value.toString());
                            }
                        }
                    };
                }
                return av;
            }
        };
    }

    @Override
    public void scanFinished(String className) {

    }
}

