package act.cli.meta;

import act.asm.Type;
import act.util.AsmTypes;
import act.util.DestroyableBase;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.List;
import java.util.Map;

import static act.Destroyable.Util.destroyAll;

public class CommanderClassMetaInfoManager extends DestroyableBase {

    private static final Logger logger = LogManager.get(CommanderClassMetaInfoManager.class);

    private Map<String, CommanderClassMetaInfo> commands = C.newMap();
    private Map<Type, List<CommanderClassMetaInfo>> subTypeInfo = C.newMap();

    public CommanderClassMetaInfoManager() {
    }

    @Override
    protected void releaseResources() {
        destroyAll(commands.values());
        commands.clear();
        for (List<CommanderClassMetaInfo> l : subTypeInfo.values()) {
            destroyAll(l);
        }
        subTypeInfo.clear();
        super.releaseResources();
    }

    public void registerCommanderMetaInfo(CommanderClassMetaInfo metaInfo) {
        if (!metaInfo.hasCommand()) {
            return;
        }
        String className = Type.getObjectType(metaInfo.className()).getClassName();
        commands.put(className, metaInfo);
        Type superType = metaInfo.superType();
        if (!AsmTypes.OBJECT_TYPE.equals(superType)) {
            CommanderClassMetaInfo superInfo = commanderMetaInfo(superType.getClassName());
            if (null != superInfo) {
                metaInfo.parent(superInfo);
            } else {
                List<CommanderClassMetaInfo> subTypes = subTypeInfo.get(superType);
                if (null == subTypes) {
                    subTypes = C.newList();
                }
                subTypes.add(metaInfo);
            }
        }
        List<CommanderClassMetaInfo> subTypes = subTypeInfo.get(metaInfo.type());
        if (null != subTypes) {
            for (CommanderClassMetaInfo subTypeInfo : subTypes) {
                subTypeInfo.parent(metaInfo);
            }
            subTypeInfo.remove(metaInfo.type());
        }
        logger.trace("Commander meta info registered for: %s", className);
    }

    public CommanderClassMetaInfo commanderMetaInfo(String className) {
        return commands.get(className);
    }

}
