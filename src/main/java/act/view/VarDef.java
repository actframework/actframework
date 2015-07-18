package act.view;

import act.util.ActContext;
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
        E.NPE(type);
        this.name = name;
        this.type = type.getCanonicalName().replace('$', '.');
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
}
