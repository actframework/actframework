package act.db;

import act.app.App;
import act.app.DbServiceManager;
import act.asm.AnnotationVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;

/**
 * Add {@code public static Dao dao()} method to the entity model class
 */
public class EntityClassEnhancer extends AppByteCodeEnhancer<EntityClassEnhancer> {

    private String classDesc;

    private boolean daoMethodFound;

    private boolean isEntityClass;

    private DbServiceManager dbm;

    public EntityClassEnhancer() {
        super($.F.<String>yes());
    }

    @Override
    protected Class<EntityClassEnhancer> subClass() {
        return EntityClassEnhancer.class;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        classDesc = "L" + name + ";";
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        Type type = Type.getType(desc);
        for (DbService dbService : dbm().registeredServices()) {
            if (type.equals(Type.getType(dbService.entityAnnotationType()))) {
                isEntityClass = true;
            }
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (isEntityClass) {
            if ("dao".equals(name) && "()Lact/db/Dao;".equals(desc)) {
                daoMethodFound = true;
                logger.warn("dao() method already defined in the model class");
            }
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (isEntityClass && !daoMethodFound) {
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
        super.visitEnd();
    }

    private synchronized DbServiceManager dbm() {
        if (null == dbm) {
            dbm = App.instance().dbServiceManager();
        }
        return dbm;
    }
}
