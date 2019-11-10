package act.test.verifier;

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

import org.osgl.$;
import org.osgl.util.E;

public abstract class Compare extends Verifier {

    public enum Type {
        GT() {
            @Override
            public boolean test(int delta) {
                return delta < 0;
            }
        },
        GTE() {
            @Override
            public boolean test(int delta) {
                return delta <= 0;
            }
        },
        LT() {
            @Override
            public boolean test(int delta) {
                return delta > 0;
            }
        },
        LTE() {
            @Override
            public boolean test(int delta) {
                return delta >= 0;
            }
        };

        public abstract boolean test(int delta);

        public <T extends Comparable> boolean applyTo(T expected, T found) {
            return test(expected.compareTo(found));
        }
    }

    private Type type;

    protected Compare(Type type) {
        this.type = $.requireNotNull(type);
    }

    @Override
    public void init(Object expected) {
        boolean isComparable = expected instanceof Comparable;
        E.illegalArgumentIf(!isComparable, "expected value must be either a Comparable");
        super.init(expected);
    }

    @Override
    public boolean verify(Object value) {
        E.illegalArgumentIfNot(value instanceof Comparable);
        Class<?> commonSuperType = $.commonSuperTypeOf(initVal, value);
        E.illegalArgumentIfNot(Comparable.class.isAssignableFrom(commonSuperType), "Expected value (%s) cannot be compared to found value (%s)", initVal, value);
        Comparable expected = (Comparable) this.initVal;
        Comparable found = (Comparable) value;
        return type.applyTo(expected, found);
    }
}
