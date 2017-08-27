package act.view;

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

import act.util.ActContext;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Defines an implicit variable
 */
public abstract class VarDef {
    private String name;
    private String type;

    /**
     * Construct an implicit variable by name and type
     *
     * @param name the name of the variable. Could be referenced in
     *             view template to get the variable
     * @param type the type of the variable. Some view solution e.g.
     *             Rythm needs to explicitly declare the template
     *             arguments. And type information is used by those
     *             static template engines
     */
    protected VarDef(String name, Class<?> type) {
        E.illegalArgumentIf(S.blank(name), "VarDef name cannot be empty");
        this.name = name;
        this.type = type.getCanonicalName().replace('$', '.');
    }

    /**
     * Construct an implicit variable by name and {@link BeanSpec bean spec}
     *
     * @param name the name of the variable. Could be referenced in
     *             view template to get the variable
     * @param type the {@link BeanSpec} of the variable. Some view solution e.g.
     *             Rythm needs to explicitly declare the template
     *             arguments. And type information is used by those
     *             static template engines
     */
    protected VarDef(String name, BeanSpec type) {
        E.illegalArgumentIf(S.blank(name), "VarDef name cannot be empty");
        this.name = name;
        this.type = toString(type);
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    /**
     * A specific variable implementation shall override this method to
     * evaluate the variable value at runtime
     *
     * @param context The application context
     * @return the variable value
     */
    public abstract Object evaluate(ActContext context);

    @Override
    public String toString() {
        return S.fmt("%s|%s", name, type);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof VarDef) {
            VarDef that = (VarDef) obj;
            return that.name.equals(name);
        }
        return false;
    }

    private static String toString(BeanSpec spec) {
        if (spec.typeParams().isEmpty()) {
            return spec.rawType().getName();
        } else {
            return spec.type().toString();
        }
    }


}
