package act.controller.meta;

import org.osgl._;
import act.asm.Type;
import act.controller.bytecode.ControllerScanner;
import act.util.AsmTypes;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.List;
import java.util.Map;

@Deprecated
/**
 * @see ControllerClassMetaInfoManager2
 */
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
        return scanForControllerMetaInfo(className, true);
    }

    public ControllerClassMetaInfo scanForParentMetaInfo(String className) {
        return scanForControllerMetaInfo(className, false);
    }

    public void mergeActionMetaInfo() {
        for (ControllerClassMetaInfo info : controllers.values()) {
            info.merge(this);
        }
    }

    private ControllerClassMetaInfo scanForControllerMetaInfo(String className, boolean verify) {
        ControllerClassMetaInfo info = this.controllerMetaInfo(className);
        if (null != info) return info;
        ControllerScanner scanner = actionScannerFactory.create();
        info = scanner.manager(this).scan(className);
        if (!verify || info.isController()) {
            Type superType = info.superType();
            if (!AsmTypes.OBJECT_TYPE.equals(superType)) {
                ControllerClassMetaInfo superInfo = scanForParentMetaInfo(superType.getClassName());
                info.parent(superInfo);
                registerControllerMetaInfo(superInfo);
            }
            List<String> withList = info.withList();
            for (String withType : withList) {
                ControllerClassMetaInfo withInfo = scanForParentMetaInfo(withType);
                registerControllerMetaInfo(withInfo);
            }
            registerControllerMetaInfo(info);
        }
        return info;
    }


}
