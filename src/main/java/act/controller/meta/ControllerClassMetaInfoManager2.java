package act.controller.meta;

import act.asm.Type;
import org.osgl.util.C;

import java.util.Map;

public class ControllerClassMetaInfoManager2 {

    private Map<String, ControllerClassMetaInfo> controllers = C.newMap();

    public ControllerClassMetaInfoManager2() {
    }

    public void registerControllerMetaInfo(ControllerClassMetaInfo metaInfo) {
        String className = Type.getObjectType(metaInfo.className()).getClassName();
        controllers.put(className, metaInfo);
    }

    public ControllerClassMetaInfo controllerMetaInfo(String className) {
        return controllers.get(className);
    }

    public void mergeActionMetaInfo() {
        for (ControllerClassMetaInfo info : controllers.values()) {
            info.merge(this);
        }
    }

}
