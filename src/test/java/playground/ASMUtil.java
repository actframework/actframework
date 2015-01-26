package playground;

import org.osgl._;
import org.osgl.mvc.server.asm.Type;

/**
 * Created by luog on 18/01/2015.
 */
public class ASMUtil {
    public static Class typeDescToClass(String desc) {
        String className = Type.getType(desc).getClassName();
        return _.classForName(className);
    }
}
