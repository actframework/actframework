package act.controller.meta;

import org.osgl.util.C;

import java.util.Map;

/**
 * Keep track all meta info about Controller class and action methods
 */
public class ControllerMetaInfoCache {
    private Map<String, ControllerClassMetaInfo> map = C.newMap();

    public ControllerClassMetaInfo get(String typeName) {
        return map.get(typeName);
    }

    public ControllerMetaInfoCache put(String typeName, ControllerClassMetaInfo info) {
        map.put(typeName, info);
        return this;
    }
}
