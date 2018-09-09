package act.db.util;

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

import act.db.CriteriaOperator;
import org.osgl.$;

import java.util.ArrayList;
import java.util.List;

public class DefaultCriteriaOperatorImpl implements CriteriaOperator {

    private String name;
    private List<String> aliases = new ArrayList<>();
    private int cardinality;
    private boolean requireCollection;
    private String negate;
    private volatile CriteriaOperator negateOperator;

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> aliases() {
        return aliases;
    }

    @Override
    public int cardinality() {
        return cardinality;
    }

    @Override
    public boolean requireCollection() {
        return requireCollection;
    }

    @Override
    public CriteriaOperator negate() {
        if (null == negateOperator) {
            synchronized (this) {
                if (null == negateOperator) {
                    negateOperator = CriteriaOperatorRegistry.INSTANCE.eval(negate);
                }
            }
        }
        return negateOperator;
    }

    @Override
    public int hashCode() {
        return $.hc(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CriteriaOperator) {
            CriteriaOperator that = (CriteriaOperator) obj;
            return $.eq(that.name(), name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
