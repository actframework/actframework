package act.inject.param;

import java.util.HashMap;
import java.util.Map;

/**
 * The base class of system generated data transfer object to support converting JSON string into
 * handler method parameters (or plus controller class fields)
 */
public abstract class JsonDTO {

    private Map<String, Object> beans = new HashMap<String, Object>();

    /**
     * Called by {@link ParamValueLoader} to get the bean
     * @param name the name of field or param
     * @return the bean
     */
    public Object get(String name) {
        return beans.get(name);
    }

    /**
     * Called by generated `setXxx(T bean)` method
     * @param name the name of the param or field
     * @param bean the bean instance
     */
    protected void set(String name, Object bean) {
        beans.put(name, bean);
    }
}
