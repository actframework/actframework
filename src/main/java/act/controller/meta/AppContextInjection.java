package act.controller.meta;

import act.app.AppContext;
import org.osgl._;
import org.osgl.util.S;

/**
 * Keep all information required to inject {@link AppContext}
 * into the controller action handler
 */
public class AppContextInjection<T> {

    /**
     * Define how framework should inject AppContext to the
     * controller action handler
     */
    public static enum InjectType {
        /**
         * Inject AppContext into controller instance field. This injection
         * is used when both of the following requirements are met
         * <ul>
         * <li>The controller has a field with type {@link AppContext}</li>
         * <li>The action handler method is not {@code static}</li>
         * </ul>
         * <p>Framework must instantiate an new instance of the
         * controller before calling the action handler method</p>
         */
        FIELD,

        /**
         * Pass AppContext via controller action method call. This injection
         * is used when there are parameter of type AppContext in the action
         * handler method signature
         */
        PARAM,

        /**
         * Save AppContext to {@link org.osgl.concurrent.ContextLocal}. If none of
         * the {@link #FIELD} and {@link #PARAM} can be used to inject the
         * {@code AppContext}, then framework shall call {@link AppContext#saveLocal}
         * method to save the app context instance into thread local variable, such that the
         * application developer could use {@link AppContext#current} method to
         * access the current application context
         */
        LOCAL;

        public boolean isLocal() {
            return this == LOCAL;
        }

        public boolean isField() {
            return this == FIELD;
        }

        public boolean isParam() {
            return this == PARAM;
        }
    }

    private InjectType type;
    protected T v;

    private AppContextInjection(InjectType type, T v) {
        this.type = type;
        this.v = v;
    }

    public InjectType injectVia() {
        return type;
    }

    @Override
    public int hashCode() {
        return _.hc(type, v);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AppContextInjection) {
            AppContextInjection that = (AppContextInjection) obj;
            return that.type == type && _.eq(that.v, v);
        }
        return false;
    }

    @Override
    public String toString() {
        return S.fmt("inject[%s, %s]", type.name().toLowerCase(), v);
    }

    public static class FieldAppContextInjection extends AppContextInjection<String> {
        FieldAppContextInjection(String fieldName) {
            super(InjectType.FIELD, fieldName);
        }

        public String fieldName() {
            return v;
        }
    }

    public static class ParamAppContextInjection extends AppContextInjection<Integer> {

        private int lvLookupIdx;

        ParamAppContextInjection(Integer paramIndex) {
            super(InjectType.PARAM, paramIndex);
        }

        public int paramIndex() {
            return v;
        }

        public ParamAppContextInjection lvLookupIdx(int index) {
            this.lvLookupIdx = index;
            return this;
        }

        public int lvLookupIdx() {
            return lvLookupIdx;
        }
    }

    public static class LocalAppContextInjection extends AppContextInjection<Void> {
        LocalAppContextInjection() {
            super(InjectType.LOCAL, null);
        }

        @Override
        public String toString() {
            return "inject[local]";
        }
    }

}
