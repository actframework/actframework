package act.db;

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

import org.osgl.util.C;

import java.util.List;

public enum CriteriaGroupLogic {
    AND() {
        @Override
        public CriteriaGroupLogic negate() {
            return OR;
        }

        @Override
        public String toString() {
            return " && ";
        }

        @Override
        public List<String> aliases() {
            return C.list("&&");
        }
    },
    OR () {
        @Override
        public CriteriaGroupLogic negate() {
            return AND;
        }

        @Override
        public String toString() {
            return " || ";
        }

        @Override
        public List<String> aliases() {
            return C.list("||");
        }
    };

    public abstract CriteriaGroupLogic negate();

    public abstract List<String> aliases();

    public static CriteriaGroupLogic valueOfIgnoreCase(String op) {
        return valueOf(op.toUpperCase());
    }
}
