package act.db.meta;

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
import act.asm.*;
import act.db.*;
import act.util.ByteCodeVisitor;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * Scans classes and build up index for quickly access the
 * entity class info.
 *
 * @see EntityMetaInfoRepo
 */
public class EntityInfoByteCodeScanner extends AppByteCodeScannerBase {
    private static final Logger LOGGER = LogManager.get(EntityMetaInfoRepo.class);

    private static final String DESC_CREATED_AT = Type.getDescriptor(CreatedAt.class);
    private static final String DESC_LAST_MODIFIED_AT = Type.getDescriptor(LastModifiedAt.class);
    private static final String DESC_CREATED_BY = Type.getDescriptor(CreatedBy.class);
    private static final String DESC_LAST_MODIFIED_BY = Type.getDescriptor(LastModifiedBy.class);
    private static final String DESC_ID = Type.getDescriptor(Id.class);
    private static final String DESC_COLUMN = Type.getDescriptor(Column.class);

    private EntityMetaInfoRepo repo;

    @Override
    protected void onAppSet() {
        repo = app().entityMetaInfoRepo();
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {

        boolean isMappedSuperClass;
        boolean isEntity;
        boolean isEntityListener;
        String className;
        boolean foundCreatedAt;
        boolean foundCreatedBy;
        boolean foundLastModifiedAt;
        boolean foundLastModifiedBy;
        boolean foundId;
        MasterEntityMetaInfoRepo metaInfoRepo;

        public _ByteCodeVisitor(ClassVisitor cv) {
            super(cv);
        }

        public _ByteCodeVisitor() {
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            className = Type.getObjectType(name).getClassName();
            metaInfoRepo = app().entityMetaInfoRepo();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (!isEntity && !isMappedSuperClass) {
                isEntity = metaInfoRepo.isEntity(desc);
                isMappedSuperClass = metaInfoRepo.isMappedSuperClass(desc);
                if (isEntity || isMappedSuperClass) {
                    repo.registerEntityOrMappedSuperClass(className);
                    if (isEntity) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public void visit(String name, Object value) {
                                if ("name".equals(name) || "value".equals(name)) {
                                    repo.registerEntityName(className, (String) value);
                                }
                                super.visit(name, value);
                            }
                        };
                    }
                }
            } else if (!isEntityListener) {
                isEntityListener = metaInfoRepo.isEntityListener(desc);
                if (isEntityListener) {
                    repo.markEntityListenersFound(className);
                }
            }
            return av;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            FieldVisitor fv = super.visitField(access, name, desc, signature, value);
            if (!isEntity && !isMappedSuperClass) {
                return fv;
            }
            final String fieldName = name;
            return new FieldVisitor(ASM5, fv) {

                String columnName;

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    AnnotationVisitor av = super.visitAnnotation(desc, visible);
                    if (S.eq(DESC_CREATED_AT, desc)) {
                        if (foundCreatedAt) {
                            LOGGER.warn("multiple @CreatedAt field found in class: " + className);
                        } else {
                            repo.registerCreatedAtField(className, fieldName);
                        }
                        foundCreatedAt = true;
                    } else if (S.eq(DESC_LAST_MODIFIED_AT, desc)) {
                        if (foundLastModifiedAt) {
                            LOGGER.warn("multiple @LastModifiedAt field found in class: " + className);
                        } else {
                            repo.registerLastModifiedAtField(className, fieldName);
                        }
                        foundLastModifiedAt = true;
                    } else if (S.eq(DESC_ID, desc)) {
                        if (foundId) {
                            LOGGER.warn("multiple @Id field found in class: " + className);
                        } else {
                            repo.registerIdField(className, fieldName);
                        }
                        foundId = true;
                    } else if (S.eq(DESC_CREATED_BY, desc)) {
                        if (foundCreatedBy) {
                            LOGGER.warn("multiple @CreatedBy field found in class: " + className);
                        } else {
                            repo.registerCreatedByField(className, fieldName);
                        }
                        foundCreatedBy = true;
                    } else if (S.eq(DESC_LAST_MODIFIED_BY, desc)) {
                        if (foundLastModifiedBy) {
                            LOGGER.warn("multiple @LastModifiedBy field found in class: " + className);
                        } else {
                            repo.registerLastModifiedByField(className, fieldName);
                        }
                        foundLastModifiedBy = true;
                    } else if (null != columnName && S.eq(DESC_COLUMN, desc)) {
                        return new AnnotationVisitor(ASM5, av) {
                            @Override
                            public void visit(String name, Object value) {
                                if (null != columnName && "name".equals(name)) {
                                    columnName = (String) value;
                                }
                            }
                        };
                    }
                    return av;
                }

                @Override
                public void visitEnd() {
                    if (null != columnName) {
                        repo.registerColumnName(className, fieldName, columnName);
                    }
                    super.visitEnd();
                }
            };
        }
    }
}
