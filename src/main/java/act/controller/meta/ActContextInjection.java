package act.controller.meta;

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

import act.app.ActionContext;
import org.osgl.$;
import org.osgl.util.S;

/**
 * Keep all information required to inject {@link ActionContext}
 * into the controller action handler
 */
public class ActContextInjection<T> {

    /**
     * Define how framework should inject AppContext to the
     * controller action handler
     */
    public enum InjectType {
        /**
         * Inject AppContext into controller instance field. This injection
         * is used when both of the following requirements are met
         * <ul>
         * <li>The controller has a field with type {@link ActionContext}</li>
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
         * {@code AppContext}, then framework shall call {@link ActionContext#saveLocal}
         * method to save the app context instance into thread local variable, such that the
         * application developer could use {@link ActionContext#current} method to
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

    private ActContextInjection(InjectType type, T v) {
        this.type = type;
        this.v = v;
    }

    public InjectType injectVia() {
        return type;
    }

    @Override
    public int hashCode() {
        return $.hc(type, v);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ActContextInjection) {
            ActContextInjection that = (ActContextInjection) obj;
            return that.type == type && $.eq(that.v, v);
        }
        return false;
    }

    @Override
    public String toString() {
        return S.concat("inject[", type.name().toLowerCase(), ", ", S.string(v), "]");
    }

    public static class FieldActContextInjection extends ActContextInjection<String> {
        public FieldActContextInjection(String fieldName) {
            super(InjectType.FIELD, fieldName);
        }

        public String fieldName() {
            return v;
        }
    }

    public static class ParamAppContextInjection extends ActContextInjection<Integer> {

        private int lvLookupIdx;

        public ParamAppContextInjection(Integer paramIndex) {
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

    public static class LocalAppContextInjection extends ActContextInjection<Void> {
        public LocalAppContextInjection() {
            super(InjectType.LOCAL, null);
        }

        @Override
        public String toString() {
            return "inject[local]";
        }
    }

}
