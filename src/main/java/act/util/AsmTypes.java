package act.util;

import act.app.ActionContext;
import act.app.App;
import act.asm.Opcodes;
import act.asm.Type;
import act.conf.AppConfig;
import act.mail.MailerContext;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;
import org.osgl.util.S;

public enum AsmTypes implements Opcodes {
    ;
    public static AsmType<Object> OBJECT = new AsmType<Object>(Object.class);
    public static AsmType<ActContext> ACT_CONTEXT = new AsmType<ActContext>(ActContext.class);
    public static AsmType<ActionContext> ACTION_CONTEXT = new AsmType<ActionContext>(ActionContext.class);
    public static AsmType<MailerContext> MAILER_CONTEXT = new AsmType<MailerContext>(MailerContext.class);
    public static AsmType<App> APP = new AsmType<App>(App.class);
    public static AsmType<AppConfig> APP_CONFIG = new AsmType<AppConfig>(AppConfig.class);
    public static AsmType<Param> PARAM = new AsmType<Param>(Param.class);
    public static AsmType<Bind> BIND = new AsmType<Bind>(Bind.class);
    public static AsmType<Result> RESULT = new AsmType<Result>(Result.class);

    public static final Type OBJECT_TYPE = OBJECT.asmType();
    public static final String OBJECT_NAME = OBJECT.className();
    public static final String OBJECT_INTERNAL_NAME = OBJECT.internalName();
    public static final String OBJECT_DESC = OBJECT.desc();

    public static final Type APP_TYPE = APP.asmType();
    public static final String APP_NAME = APP.className();
    public static final String APP_INTERNAL_NAME = APP.internalName();
    public static final String APP_DESC = APP.desc();

    public static final Type APP_CONFIG_TYPE = APP_CONFIG.asmType();
    public static final String APP_CONFIG_NAME = APP_CONFIG.className();
    public static final String APP_CONFIG_INTERNAL_NAME = APP_CONFIG.internalName();
    public static final String APP_CONFIG_DESC = APP_CONFIG.desc();

    public static final Type MAILER_CONTEXT_TYPE = MAILER_CONTEXT.asmType();
    public static final String MAILER_CONTEXT_NAME = MAILER_CONTEXT.className();
    public static final String MAILER_CONTEXT_INTERNAL_NAME = MAILER_CONTEXT.internalName();
    public static final String MAILER_CONTEXT_DESC = MAILER_CONTEXT.desc();

    public static final Type ACT_CONTEXT_TYPE = ACT_CONTEXT.asmType();
    public static final String ACT_CONTEXT_NAME = ACT_CONTEXT.className();
    public static final String ACT_CONTEXT_INTERNAL_NAME = ACT_CONTEXT.internalName();
    public static final String ACT_CONTEXT_DESC = ACT_CONTEXT.desc();

    public static final Type ACTION_CONTEXT_TYPE = ACTION_CONTEXT.asmType();
    public static final String ACTION_CONTEXT_NAME = ACTION_CONTEXT.className();
    public static final String ACTION_CONTEXT_INTERNAL_NAME = ACTION_CONTEXT.internalName();
    public static final String ACTION_CONTEXT_DESC = ACTION_CONTEXT.desc();

    public static final Type PARAM_TYPE = PARAM.asmType();
    public static final String PARAM_NAME = PARAM.className();
    public static final String PARAM_INTERNAL_NAME = PARAM.internalName();
    public static final String PARAM_DESC = PARAM.desc();

    public static final Type BIND_TYPE = BIND.asmType();
    public static final String BIND_NAME = BIND.className();
    public static final String BIND_INTERNAL_NAME = BIND.internalName();
    public static final String BIND_DESC = BIND.desc();

    public static final Type RESULT_TYPE = RESULT.asmType();
    public static final String RESULT_NAME = RESULT.className();
    public static final String RESULT_INTERNAL_NAME = RESULT.internalName();
    public static final String RESULT_DESC = RESULT.desc();

    public static String methodDesc(Class retType, Class... paramTypes) {
        StringBuilder sb = S.builder("(");
        for (Class c : paramTypes) {
            Type t = Type.getType(c);
            sb.append(t.getDescriptor());
        }
        sb.append(")");
        if (Void.class.equals(retType)) {
            sb.append(Type.VOID_TYPE.getDescriptor());
        } else {
            sb.append(Type.getType(retType).getDescriptor());
        }
        return sb.toString();
    }

    public static boolean isStatic(int access) {
        return (ACC_STATIC & access) > 0;
    }

}
