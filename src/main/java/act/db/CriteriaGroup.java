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

import act.db.util.CriteriaUtil;
import org.osgl.$;
import org.osgl.util.*;

import java.util.ArrayList;
import java.util.List;

public class CriteriaGroup implements CriteriaComponent {
    private CriteriaGroupLogic logic;
    private List<CriteriaComponent> components;

    public CriteriaGroup(CriteriaGroupLogic logic, CriteriaComponent... components) {
        this.logic = $.requireNotNull(logic);
        E.illegalArgumentIf(components.length == 0, "At least one component needed");
        this.components = C.listOf(components);
    }

    public CriteriaGroup(CriteriaGroupLogic logic, List<CriteriaComponent> components) {
        this.logic = $.requireNotNull(logic);
        this.components = $.requireNotNull(components);
    }

    private CriteriaGroup(CriteriaGroup copy) {
        this.logic = copy.logic;
        this.components = new ArrayList(copy.components);
    }

    @Override
    public int hashCode() {
        return $.hc(logic, components);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CriteriaGroup) {
            CriteriaGroup that = (CriteriaGroup) obj;
            return that.logic == logic && $.eq(that.components, components);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = S.builder();
        print(builder);
        return builder.toString();
    }

    @Override
    public StringBuilder print(StringBuilder buffer) {
        String op = logic.toString();
        buffer.append("(");
        boolean first = true;
        for (CriteriaComponent comp : components) {
            if (!first) {
                buffer.append(op);
            } else {
                first = false;
            }
            comp.print(buffer);
        }
        buffer.append(")");
        return buffer;
    }

    public CriteriaGroupLogic getLogic() {
        return logic;
    }

    public List<CriteriaComponent> getComponents() {
        return components;
    }

    @Override
    public CriteriaGroup negate() {
        CriteriaGroup newGroup = new CriteriaGroup(this);
        newGroup.logic = this.logic.negate();
        int len = components.size();
        for (int i = 0; i < len; ++i) {
            newGroup.components.set(i, components.get(i).negate());
        }
        return newGroup;
    }

    public CriteriaGroup and(CriteriaComponent... otherComponents) {
        return CriteriaUtil.and(this, otherComponents);
    }

    public CriteriaGroup or(CriteriaComponent... otherComponents) {
        return CriteriaUtil.or(this, otherComponents);
    }

    @Override
    public void accept(CriteriaVisitor visitor) {
        visitor.beginGroup(this);
        for (CriteriaComponent component : components) {
            component.accept(visitor);
        }
        visitor.endGroup();
    }
}
