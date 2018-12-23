package act.db.util;

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

import act.Act;
import act.app.App;
import act.app.event.SysEventId;
import act.db.*;
import act.db.meta.*;
import act.plugin.PrincipalProvider;
import act.util.*;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.util.E;

import java.lang.reflect.Field;
import java.util.*;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@Stateless
public class AuditHelper {
    private Map<Class, $.Visitor> createdAtLookup = new HashMap<>();
    private Map<Class, $.Predicate> createdAtCheckLookup = new HashMap<>();
    private Map<Class, $.Visitor> lastModifiedAtLookup = new HashMap<>();
    private Map<Class, $.Visitor> createdByLookup = new HashMap<>();
    private Map<Class, $.Predicate> createdByCheckLookup = new HashMap<>();
    private Map<Class, $.Visitor> lastModifiedByLookup = new HashMap<>();

    public AuditHelper() {
        buildLookups();
        final App app = Act.app();
        final AuditHelper me = this;
        // register singleton as the first AuditHelper could be initialized
        // by ORM lib e.g Hibernate
        app.jobManager().on(SysEventId.SINGLETON_PROVISIONED, new Runnable() {
            @Override
            public void run() {
                app.registerSingleton(me);
            }
        }, true);
    }

    @PrePersist
    public void prePersist(Object entity) {
        Class<?> entityType = entity.getClass();
        apply(createdAtLookup.get(entityType), entity);
        apply(lastModifiedAtLookup.get(entityType), entity);
        apply(createdByLookup.get(entityType), entity);
        apply(lastModifiedByLookup.get(entityType), entity);
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        Class<?> entityType = entity.getClass();
        apply(lastModifiedAtLookup.get(entityType), entity);
        apply(lastModifiedByLookup.get(entityType), entity);
    }

    protected boolean hasCreatedAtValue(Object entity) {
        Class<?> entityType = entity.getClass();
        $.Predicate predicate = createdAtCheckLookup.get(entityType);
        return null == predicate ? false : predicate.test(entity);
    }

    protected boolean hasCreatedByValue(Object entity) {
        Class<?> entityType = entity.getClass();
        $.Predicate predicate = createdByCheckLookup.get(entityType);
        return null == predicate ? false : predicate.test(entity);
    }

    private void buildLookups() {
        final App app = Act.app();
        MasterEntityMetaInfoRepo masterRepo = app.entityMetaInfoRepo();
        final DbManager dbManager = Act.dbManager();
        ClassInfoRepository classInfoRepository = app.classLoader().classInfoRepository();
        for (DbService db : app.dbServiceManager().registeredServices()) {
            final EntityMetaInfoRepo repo = masterRepo.forDb(db.id());
            final Set<Class> entityClasses = new HashSet<>(repo.entityClasses());
            for (Class entityClass : repo.entityClasses()) {
                if (!entityClasses.contains(entityClass)) {
                    continue;
                }
                final EntityClassMetaInfo classInfo = repo.classMetaInfo(entityClass);
                EntityFieldMetaInfo fieldInfo = classInfo.createdAtField();
                if (null != fieldInfo) {
                    final Field field = $.requireNotNull($.fieldOf(entityClass, fieldInfo.fieldName()));
                    field.setAccessible(true);
                    final TimestampFieldVisitor timestampFieldVisitor = new TimestampFieldVisitor(field, dbManager);
                    createdAtLookup.put(entityClass, timestampFieldVisitor);
                    createdAtCheckLookup.put(entityClass, new FieldChecker(field));
                    final ClassNode node = classInfoRepository.node(entityClass.getName());
                    node.visitSubTree(new $.Visitor<ClassNode>() {
                        @Override
                        public void visit(ClassNode classNode) throws $.Break {
                            String className = classNode.name();
                            EntityClassMetaInfo classInfo = repo.classMetaInfo(className);
                            if (null == classInfo) {
                                return;
                            }
                            EntityFieldMetaInfo fieldInfo = classInfo.createdAtField();
                            if (null == fieldInfo) {
                                Class entityClass = app.classForName(className);
                                createdAtLookup.put(entityClass, timestampFieldVisitor);
                                entityClasses.remove(entityClass);
                            }
                        }
                    }, true, true);
                }
                fieldInfo = classInfo.createdByField();
                if (null != fieldInfo) {
                    final Field field = $.requireNotNull($.fieldOf(entityClass, fieldInfo.fieldName()));
                    field.setAccessible(true);
                    final PricipalFieldVisitor pricipalFieldVisitor = new PricipalFieldVisitor(field);
                    createdByLookup.put(entityClass, pricipalFieldVisitor);
                    createdByCheckLookup.put(entityClass, new FieldChecker(field));
                    final ClassNode node = classInfoRepository.node(entityClass.getName());
                    node.visitSubTree(new $.Visitor<ClassNode>() {
                        @Override
                        public void visit(ClassNode classNode) throws $.Break {
                            String className = classNode.name();
                            EntityClassMetaInfo classInfo = repo.classMetaInfo(className);
                            if (null == classInfo) {
                                return;
                            }
                            EntityFieldMetaInfo fieldInfo = classInfo.createdByField();
                            if (null == fieldInfo) {
                                Class entityClass = app.classForName(className);
                                createdByLookup.put(entityClass, pricipalFieldVisitor);
                                entityClasses.remove(entityClass);
                            }
                        }
                    }, true, true);
                }
                fieldInfo = classInfo.lastModifiedAtField();
                if (null != fieldInfo) {
                    Field field = $.requireNotNull($.fieldOf(entityClass, fieldInfo.fieldName()));
                    final TimestampFieldVisitor timestampFieldVisitor = new TimestampFieldVisitor(field, dbManager);
                    lastModifiedAtLookup.put(entityClass, timestampFieldVisitor);
                    final ClassNode node = classInfoRepository.node(entityClass.getName());
                    node.visitSubTree(new $.Visitor<ClassNode>() {
                        @Override
                        public void visit(ClassNode classNode) throws $.Break {
                            String className = classNode.name();
                            EntityClassMetaInfo classInfo = repo.classMetaInfo(className);
                            if (null == classInfo) {
                                return;
                            }
                            EntityFieldMetaInfo fieldInfo = classInfo.createdAtField();
                            if (null == fieldInfo) {
                                Class entityClass = app.classForName(className);
                                lastModifiedAtLookup.put(entityClass, timestampFieldVisitor);
                                entityClasses.remove(entityClass);
                            }
                        }
                    }, true, true);
                }
                fieldInfo = classInfo.lastModifiedByField();
                if (null != fieldInfo) {
                    Field field = $.requireNotNull($.fieldOf(entityClass, fieldInfo.fieldName()));
                    final PricipalFieldVisitor pricipalFieldVisitor = new PricipalFieldVisitor(field);
                    lastModifiedByLookup.put(entityClass, pricipalFieldVisitor);
                    final ClassNode node = classInfoRepository.node(entityClass.getName());
                    node.visitSubTree(new $.Visitor<ClassNode>() {
                        @Override
                        public void visit(ClassNode classNode) throws $.Break {
                            String className = classNode.name();
                            EntityClassMetaInfo classInfo = repo.classMetaInfo(className);
                            if (null == classInfo) {
                                return;
                            }
                            EntityFieldMetaInfo fieldInfo = classInfo.createdByField();
                            if (null == fieldInfo) {
                                Class entityClass = app.classForName(className);
                                lastModifiedByLookup.put(entityClass, pricipalFieldVisitor);
                                entityClasses.remove(entityClass);
                            }
                        }
                    }, true, true);
                }
            }
        }
    }

    private static void apply($.Visitor visitor, Object entity) {
        if (null != visitor) {
            visitor.visit(entity);
        }
    }

    private static class FieldChecker extends $.Predicate {

        final Field field;

        FieldChecker(Field field) {
            this.field = field;
        }

        @Override
        public boolean test(Object o) {
            return null != $.getFieldValue(o, field);
        }
    }

    private static class TimestampFieldVisitor extends $.Visitor {
        final TimestampGenerator tsGen;
        final Field field;
        public TimestampFieldVisitor(Field field, DbManager dbManager) {
            this.tsGen = dbManager.timestampGenerator(field.getType());
            field.setAccessible(true);
            this.field = field;
        }

        @Override
        public void visit(Object entity) throws $.Break {
            try {
                field.set(entity, tsGen.now());
            } catch (IllegalAccessException e) {
                throw E.unexpected(e);
            }
        }
    }

    private static class PricipalFieldVisitor extends $.Visitor {
        final Field field;
        final PrincipalProvider principalProvider;

        public PricipalFieldVisitor(Field field) {
            field.setAccessible(true);
            this.field = field;
            this.principalProvider = Act.app().principalProvider();
        }

        @Override
        public void visit(Object o) throws Lang.Break {
            if (null == o) {
                return;
            }
            $.setFieldValue(o, field, principalProvider.get());
        }
    }

}
