package org.osgl.oms.controller.meta;

import org.osgl._;
import org.osgl.oms.asm.Type;
import org.osgl.oms.controller.bytecode.ControllerScanner;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.Map;

public class ControllerClassMetaInfoManager {

    private Map<String, ControllerClassMetaInfo> controllers = C.newMap();
    private _.Factory<ControllerScanner> actionScannerFactory;

    public ControllerClassMetaInfoManager(_.Func0<ControllerScanner> actionScannerFactory) {
        E.NPE(actionScannerFactory);
        this.actionScannerFactory = _.factory(actionScannerFactory);
    }

    public void registerControllerMetaInfo(ControllerClassMetaInfo metaInfo) {
        String className = Type.getObjectType(metaInfo.className()).getClassName();
        controllers.put(className, metaInfo);
    }

    public ControllerClassMetaInfo controllerMetaInfo(String className) {
        return controllers.get(className);
    }

    public ControllerClassMetaInfo scanForControllerMetaInfo(String className) {
        ControllerScanner scanner = actionScannerFactory.create();
        ControllerClassMetaInfo info = scanner.manager(this).scan(className);
        registerControllerMetaInfo(info);
        return info;
    }

    public void mergeActionMetaInfo() {
        for (ControllerClassMetaInfo info: controllers.values()) {
            info.merge(this);
        }
    }


}
