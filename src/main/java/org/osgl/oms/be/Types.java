package org.osgl.oms.be;

import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.asm.Type;
import org.osgl.util.S;

public enum Types {
    ;
    public static enum APP_CONTEXT {
        ;
        public static final Type TYPE;
        public static final String DESC;
        public static final String CLASS_NAME;
        public static final String INTERNAL_NAME;
        static {
            TYPE = Type.getType(AppContext.class);
            DESC = TYPE.getDescriptor();
            CLASS_NAME = AppContext.class.getName();
            INTERNAL_NAME = TYPE.getInternalName();
        }
    }

    public static enum PARAM {
        ;
        public static final Type TYPE;
        public static final String DESC;
        public static final String CLASS_NAME;
        public static final String INTERNAL_NAME;
        static {
            TYPE = Type.getType(Param.class);
            DESC = TYPE.getDescriptor();
            CLASS_NAME = Param.class.getName();
            INTERNAL_NAME = TYPE.getInternalName();
        }
    }

    public static enum BIND {
        ;
        public static final Type TYPE;
        public static final String DESC;
        public static final String CLASS_NAME;
        public static final String INTERNAL_NAME;
        static {
            TYPE = Type.getType(Bind.class);
            DESC = TYPE.getDescriptor();
            CLASS_NAME = Bind.class.getName();
            INTERNAL_NAME = TYPE.getInternalName();
        }
    }

    public static enum RESULT {
        ;
        public static final Type TYPE;
        public static final String DESC;
        public static final String CLASS_NAME;
        public static final String INTERNAL_NAME;
        static {
            TYPE = Type.getType(Result.class);
            DESC = TYPE.getDescriptor();
            CLASS_NAME = Result.class.getName();
            INTERNAL_NAME = TYPE.getInternalName();
        }
    }

    public static final Type APP_CONTEXT_TYPE = APP_CONTEXT.TYPE;
    public static final String APP_CONTEXT_NAME = APP_CONTEXT.CLASS_NAME;
    public static final String APP_CONTEXT_INTERNAL_NAME = APP_CONTEXT.INTERNAL_NAME;
    public static final String APP_CONTEXT_DESC = APP_CONTEXT.DESC;

    public static final Type PARAM_TYPE = PARAM.TYPE;
    public static final String PARAM_NAME = PARAM.CLASS_NAME;
    public static final String PARAM_INTERNAL_NAME = PARAM.INTERNAL_NAME;
    public static final String PARAM_DESC = PARAM.DESC;

    public static final Type BIND_TYPE = BIND.TYPE;
    public static final String BIND_NAME = BIND.CLASS_NAME;
    public static final String BIND_INTERNAL_NAME = BIND.INTERNAL_NAME;
    public static final String BIND_DESC = BIND.DESC;

    public static final Type RESULT_TYPE = RESULT.TYPE;
    public static final String RESULT_NAME = RESULT.CLASS_NAME;
    public static final String RESULT_INTERNAL_NAME = RESULT.INTERNAL_NAME;
    public static final String RESULT_DESC = RESULT.DESC;

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

}
