package act.db.meta;

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
import act.app.event.SysEventId;
import act.asm.Type;
import act.db.DB;
import act.job.JobManager;
import act.util.ClassInfoRepository;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.*;

@Singleton
public class MasterEntityMetaInfoRepo extends EntityMetaInfoRepo {

    private static EntityMetaInfoRepo EMPTY;

    // map entity meta info repo to db id
    private Map<String, EntityMetaInfoRepo> regions = new HashMap<>();

    private Set<String> entityAnnotations = new HashSet<>();
    private Set<String> mappedSuperClassAnnotations = new HashSet<>();
    private Set<String> entityListenerAnnotations = new HashSet<>();
    private String defaultAlias;

    @Inject
    public MasterEntityMetaInfoRepo(final App app) {
        super(app);
        EMPTY = new EntityMetaInfoRepo(app){
            @Override
            public void registerEntityOrMappedSuperClass(String className) {
                E.unsupport();
            }

            @Override
            public void registerEntityName(String className, String entityName) {
                E.unsupport();
            }

            @Override
            public void markEntityListenersFound(String className) {
                E.unsupport();
            }

            @Override
            public void registerCreatedAtField(String className, String fieldName) {
                E.unsupport();
            }

            @Override
            public void registerLastModifiedAtField(String className, String fieldName) {
                E.unsupport();
            }

            @Override
            public void registerIdField(String className, String fieldName) {
                E.unsupport();
            }

            @Override
            public void registerColumnName(String className, String fieldName, String columnName) {
                E.unsupport();
            }

            @Override
            public boolean isRegistered(String className) {
                return false;
            }

            @Override
            public Set<Class> entityClasses() {
                return C.Set();
            }

            @Override
            public EntityClassMetaInfo classMetaInfo(Class<?> entityClass) {
                return null;
            }

            @Override
            public EntityClassMetaInfo classMetaInfo(String className) {
                return null;
            }

            @Override
            protected void releaseResources() {
            }

            @Override
            void register(Class<?> entityClass, EntityClassMetaInfo info) {
                E.unsupport();
            }
        };
        registerEntityAnnotation(Entity.class);
        registerMappedSuperClassAnnotation(MappedSuperclass.class);
        registerEntityListenerAnnotation(EntityListeners.class);
        final MasterEntityMetaInfoRepo me = this;
        JobManager jobManager = app.jobManager();
        jobManager.on(SysEventId.CLASS_LOADED, "MasterEntityMetaInfoRepo:findAndRegisterEntityClasses", new Runnable() {
            @Override
            public void run() {
                final ClassInfoRepository classRepo = app.classLoader().classInfoRepository();
                for (Map.Entry<String, EntityClassMetaInfo> entry : lookup.entrySet()) {
                    Class<?> entityClass = app.classForName(entry.getKey());
                    EntityClassMetaInfo info = entry.getValue();
                    info.mergeFromMappedSuperClasses(classRepo, me);
                    register(entityClass, info);
                    DB db = entityClass.getAnnotation(DB.class);
                    String dbId = (null == db ? DB.DEFAULT : db.value()).toUpperCase();
                    EntityMetaInfoRepo repo = regions.get(dbId);
                    if (null == repo) {
                        repo = new EntityMetaInfoRepo(app);
                        regions.put(dbId, repo);
                    }
                    repo.register(entityClass, info);
                }
            }
        });
        jobManager.on(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED, "MasterEntityMetaInfoRepo:registerEntityMetaInfoRepo.Provider", new Runnable() {
            @Override
            public void run() {
                app.injector().registerNamedProvider(EntityMetaInfoRepo.class, app.getInstance(EntityMetaInfoRepo.Provider.class));
            }
        });
    }

    public void registerEntityAnnotation(Class<? extends Annotation> annoType) {
        entityAnnotations.add(Type.getType(annoType).getDescriptor());
    }

    public void registerMappedSuperClassAnnotation(Class<? extends Annotation> annoType) {
        mappedSuperClassAnnotations.add(Type.getType(annoType).getDescriptor());
    }

    public void registerEntityListenerAnnotation(Class<? extends Annotation> annoType) {
        entityListenerAnnotations.add(Type.getType(annoType).getDescriptor());
    }

    public void setDefaultAlias(String defaultAlias) {
        this.defaultAlias = defaultAlias;
    }


    public boolean isEntity(String descriptor) {
        return entityAnnotations.contains(descriptor);
    }

    public boolean isMappedSuperClass(String descriptor) {
        return mappedSuperClassAnnotations.contains(descriptor);
    }

    public boolean isEntityListener(String descriptor) {
        return entityListenerAnnotations.contains(descriptor);
    }

    public EntityMetaInfoRepo forDefaultDb() {
        return forDb(DB.DEFAULT);
    }

    public EntityMetaInfoRepo forDb(String dbId) {
        final String DEF = DB.DEFAULT.toUpperCase();
        if (null == dbId) {
            dbId = DEF;
        } else {
            dbId = dbId.toUpperCase();
        }
        EntityMetaInfoRepo repo = regions.get(dbId);
        if (null == repo && null != defaultAlias) {
            final String DEF_ALIAS = defaultAlias.toUpperCase();
            if (DEF.equals(dbId)) {
                dbId = DEF_ALIAS;
            } else if (DEF_ALIAS.equals(dbId)) {
                dbId = DEF;
            }
            repo = regions.get(dbId);
        }
        return null != repo ? repo : EMPTY;
    }

    public Iterable<EntityMetaInfoRepo> allRepos() {
        return regions.values();
    }

    public static EntityMetaInfoRepo EMPTY() {
        return EMPTY;
    }
}
