package act.db;

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

import act.app.App;
import act.app.DbServiceManager;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.util.AppByteCodeEnhancer;
import act.util.ClassNode;

/**
 * Add {@code public static Dao dao()} method to the entity model class
 */
public class EntityClassEnhancer extends AppByteCodeEnhancer<EntityClassEnhancer> {

    private String classDesc;
    private boolean isModel;

    private boolean daoMethodFound;
    private boolean daoClsMethodFound;

    private boolean isEntityClass;

    private DbServiceManager dbm;

    public EntityClassEnhancer() {
    }

    @Override
    protected Class<EntityClassEnhancer> subClass() {
        return EntityClassEnhancer.class;
    }

    @Override
    protected void reset() {
        this.classDesc = null;
        this.daoClsMethodFound = false;
        this.daoMethodFound = false;
        this.isEntityClass = false;
        super.reset();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        classDesc = "L" + name + ";";
        String className = Type.getType(classDesc).getClassName();
        ClassNode node = app.classLoader().classInfoRepository().node(className);
        isModel = node != null && node.hasInterface(Model.class.getName());
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (isModel) {
            Type type = Type.getType(desc);
            for (DbService dbService : dbm().registeredServices()) {
                if (type.equals(Type.getType(dbService.entityAnnotationType()))) {
                    isEntityClass = true;
                }
            }
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (isEntityClass) {
            if ("dao".equals(name)) {
                if ("()Lact/db/Dao;".equals(desc)) {
                    daoMethodFound = true;
                    logger.warn("dao() method already defined in the model class");
                }
                if ("(Ljava/lang/Class;)Lact/db/Dao;".equals(desc)) {
                    daoClsMethodFound = true;
                    logger.warn("dao(Class) method already defined in the model class");
                }
            }

        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (isEntityClass) {
            if (!daoMethodFound) {
                // add Model.dao() method
                MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC,
                        "dao", "()Lact/db/Dao;",
                        "<ID_TYPE:Ljava/lang/Object;MODEL_TYPE:Lact/db/ModelBase<TID_TYPE;TMODEL_TYPE;>;QUERY_TYPE::Lact/db/Dao$Query<TMODEL_TYPE;TQUERY_TYPE;>;DAO_TYPE::Lact/db/Dao<TID_TYPE;TMODEL_TYPE;TQUERY_TYPE;TDAO_TYPE;>;>()TDAO_TYPE;", null);
                mv.visitCode();
                mv.visitMethodInsn(INVOKESTATIC, "act/app/App", "instance", "()Lact/app/App;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "act/app/App", "dbServiceManager", "()Lact/app/DbServiceManager;", false);
                mv.visitLdcInsn(Type.getType(classDesc));
                mv.visitMethodInsn(INVOKEVIRTUAL, "act/app/DbServiceManager", "dao", "(Ljava/lang/Class;)Lact/db/Dao;", false);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(2, 0);
                mv.visitEnd();
            }
            if (!daoClsMethodFound) {
                // add Model.dao(Class c) method
                MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "dao", "(Ljava/lang/Class;)Lact/db/Dao;", "<T::Lact/db/Dao;>(Ljava/lang/Class<TT;>;)TT;", null);
                mv.visitCode();
                mv.visitMethodInsn(INVOKESTATIC, "act/app/App", "instance", "()Lact/app/App;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "act/app/App", "dbServiceManager", "()Lact/app/DbServiceManager;", false);
                mv.visitLdcInsn(Type.getType(classDesc));
                mv.visitMethodInsn(INVOKEVIRTUAL, "act/app/DbServiceManager", "dao", "(Ljava/lang/Class;)Lact/db/Dao;", false);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            }
        }
        super.visitEnd();
    }

    private synchronized DbServiceManager dbm() {
        if (null == dbm) {
            dbm = App.instance().dbServiceManager();
        }
        return dbm;
    }
}
