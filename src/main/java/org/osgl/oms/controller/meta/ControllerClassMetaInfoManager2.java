package org.osgl.oms.controller.meta;

import org.osgl._;
import org.osgl.oms.asm.Type;
import org.osgl.oms.controller.bytecode.ControllerScanner;
import org.osgl.oms.util.AsmTypes;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
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
