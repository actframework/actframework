package act.test.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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
import act.test.verifier.ReversedVerifier;
import act.test.verifier.Verifier;
import act.util.LogSupport;
import org.osgl.$;
import org.osgl.util.*;

import java.util.*;

/**
 * An `NamedLogic` encapsulate a piece of logic with a name, which can
 * be used to specify the logic in `scenarios.yml` file.
 *
 * The named logic could belong to different types, e.g.
 *
 * * Action - trigger an macro, e.g. clear current session
 * * Assert - used to verify the data
 * * Modifier - used to modify request
 */
public abstract class NamedLogic extends LogSupport {

    protected abstract Class<? extends NamedLogic> type();

    protected Object initVal;

    public void register() {
        register(false);
    }

    protected void register(boolean force) {
        Keyword keyword = keyword();
        NamedLogicRegister register = Act.getInstance(NamedLogicRegister.class);
        register.register(keyword, this, force);
        for (String alias : aliases()) {
            keyword = Keyword.of(alias);
            register.register(keyword, this, force);
        }
    }

    @Override
    public String toString() {
        if (null == initVal) {
            return keyword().hyphenated();
        }
        return S.concat(keyword().hyphenated(), ": ", initVal);
    }

    @Override
    public int hashCode() {
        return $.hc(initVal, getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        Class<?> type = obj.getClass();
        if (type != getClass()) {
            return false;
        }
        return $.eq(((NamedLogic) obj).initVal, initVal);
    }


    /**
     * Sub class can override this method to initialization work
     *
     * @param param
     *      the initialization string
     */
    public void init(Object param) {
        this.initVal = param;
    }

    /**
     * Sub class can override this method to provide aliases
     * of the logic piece.
     *
     * For example `Eq` assert provides the following aliases:
     *
     * * `equalTo`
     * * `value`
     *
     * @return a list of aliases.
     */
    protected List<String> aliases() {
        return C.list();
    }

    protected Keyword keyword() {
        String name = getClass().getSimpleName();
        return Keyword.of(name);
    }

    protected static class FromLinkedHashMap<T extends NamedLogic> extends $.TypeConverter<LinkedHashMap, T> {

        private NamedLogicRegister register = Act.getInstance(NamedLogicRegister.class);

        public FromLinkedHashMap(Class<T> toType) {
            super(LinkedHashMap.class, toType);
        }

        @Override
        public T convert(LinkedHashMap o) {
            E.illegalStateIfNot(o.size() == 1, "single element map expected");
            Map.Entry entry = (Map.Entry) o.entrySet().iterator().next();
            String key = S.string(entry.getKey());
            boolean revert = false;
            if (key.startsWith("!") || key.startsWith("-")) {
                revert = true;
                key = key.substring(1).trim();
            } else if (key.startsWith("not:") || key.startsWith("not ")) {
                revert = true;
                key = key.substring(4).trim();
            }
            T logic = register.get(toType, key);
            E.illegalArgumentIf(null == logic, "%s not found: %s", toType.getName(), key);
            logic = $.cloneOf(logic);
            logic.init(entry.getValue());
            if (revert && logic instanceof Verifier) {
                final Verifier v = $.cast(logic);
                return $.cast(new ReversedVerifier(v));
            }
            return logic;
        }
    }

    protected static class FromString<T extends NamedLogic> extends $.TypeConverter<String, T> {

        private NamedLogicRegister register = Act.getInstance(NamedLogicRegister.class);

        public FromString(Class<T> toType) {
            super(String.class, toType);
        }

        @Override
        public T convert(String o) {
            E.illegalStateIf(S.blank(o));
            String key = o;
            boolean revert = false;
            if (key.startsWith("!") || key.startsWith("-")) {
                revert = true;
                key = key.substring(1).trim();
            } else if (key.startsWith("not:") || key.startsWith("not ")) {
                revert = true;
                key = key.substring(4).trim();
            }
            T logic = register.get(toType, key);
            E.illegalArgumentIf(null == logic, "%s not found: %s", toType.getName(), key);
            logic = $.cloneOf(logic);
            if (revert && logic instanceof Verifier) {
                final Verifier v = $.cast(logic);
                return $.cast(new ReversedVerifier(v));
            }
            return logic;
        }
    }


}
