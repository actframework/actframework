package org.osgl.oms.be;

import org.osgl.mvc.result.Result;
import org.osgl.oms.AppContext;
import org.osgl.oms.asm.Label;
import org.osgl.oms.asm.Type;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.Map;

/**
 * Stores all information about an action handler method required to
 * inject code that calling this method
 */
public class ActionMethodMetaInfo {

    /**
     * How AppContext is passed into this handler.
     */
    public static enum AppContextBy {

        /**
         * AppContext been passed as a parameter
         * to the method. In this case, the framework
         * will not store the AppContext into it's
         * ThreadLocal variable
         */
        PARAM,

        /**
         * AppContext is not declared as method parameter,
         * the framework needs to store the AppContext
         * into ContextLocal storage
         */
        CONTEXT_LOCAL;

        public boolean contextLocal() {
            return CONTEXT_LOCAL == this;
        }
    }

    /**
     * Is this handler a static or virtual method
     */
    public static enum InvokeType {
        STATIC, VIRTUAL
    }

    /**
     * Does this handler returns a Result type
     * value. If not then the framework must enhance
     * the method to throw out the last statement
     * that returns a Result type value
     */
    public boolean hasReturnType;

    private String name;
    private AppContextBy appContextBy = AppContextBy.CONTEXT_LOCAL;
    /**
     * The parameter index for AppContext if there is
     */
    private int appContextIndex = -1;
    private InvokeType invokeType;
    private List<ParamMetaInfo> params = C.newList();
    private Map<Label, Map<Integer, LocalVariableMetaInfo>> locals = C.newMap();

    public ActionMethodMetaInfo name(String name) {
        this.name = name;
        return this;
    }

    public ActionMethodMetaInfo appContextBy(AppContextBy by) {
        appContextBy = by;
        return this;
    }

    public AppContextBy appContextBy() {
        return appContextBy;
    }

    public ActionMethodMetaInfo invokeType(InvokeType type) {
        this.invokeType = type;
        return this;
    }

    public ActionMethodMetaInfo returnType(Type type) {
        if (Type.VOID_TYPE.equals(type)) {
            hasReturnType = false;
        } else {
            if (Result.class.getName().equals(type.getClassName())) {
                hasReturnType = true;
            } else {
                throw E.unexpected("Either void or Result type expected, found: %s", type.getClassName());
            }
        }
        return this;
    }

    public boolean isStatic() {
        return invokeType == InvokeType.STATIC;
    }

    public ActionMethodMetaInfo appContextIndex(int index) {
        E.illegalStateIf(appContextIndex != -1, "Multiple AppContext type parameter detected");
        appContextIndex = index;
        return this;
    }

    public int appContextIndex() {
        return appContextIndex;
    }

    public ActionMethodMetaInfo addParam(ParamMetaInfo param) {
        params.add(param);
        if (APP_CONTEXT_TYPE.equals(param.type())) {
            appContextBy(AppContextBy.PARAM);
        }
        return this;
    }

    public boolean hasLocalVariableTable() {
        return !locals.isEmpty();
    }

    public ActionMethodMetaInfo addLocal(LocalVariableMetaInfo local) {
        Label start = local.start();
        Map<Integer, LocalVariableMetaInfo> m = locals.get(start);
        if (null == m) {
            m = C.newMap();
            locals.put(start, m);
        }
        int index = local.index();
        E.illegalStateIf(m.containsKey(index), "Local variable index conflict");
        m.put(local.index(), local);
        return this;
    }

    public LocalVariableMetaInfo localVariable(int index, Label start) {
        Map<Integer, LocalVariableMetaInfo> l = locals.get(start);
        if (null == l) return null;
        return l.get(index);
    }

    public ParamMetaInfo param(int id) {
        return params.get(id);
    }

    public int paramCount() {
        return params.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("***** Method Meta Info *****\n");
        sb.append(" Name: ").append(name).append("\n");
        sb.append(" AppContext by: ").append(appContextBy).append("\n");
        if (-1 != appContextIndex) {
            sb.append(" AppContext param index: ").append(appContextIndex).append("\n");
        }
        sb.append(" Invoke type: ").append(invokeType).append("\n");
        sb.append(" Has return type? ").append(hasReturnType).append("\n");
        sb.append("----- Parameters -----\n");
        for (ParamMetaInfo param : params) {
            sb.append(param).append("\n");
        }
        sb.append("----- Local variables -----\n");
        for (Map<Integer, LocalVariableMetaInfo> l : locals.values()) {
            for (LocalVariableMetaInfo local: l.values()) {
                sb.append(local).append("\n");
            }
        }
        return sb.toString();
    }

    public static final Type APP_CONTEXT_TYPE = Type.getType(AppContext.class);
}
