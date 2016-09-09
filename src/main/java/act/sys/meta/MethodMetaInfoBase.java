package act.sys.meta;

import act.util.DestroyableBase;

/**
 * Base class for storing method meta info
 */
public class MethodMetaInfoBase<T extends ClassMetaInfoBase, M extends MethodMetaInfoBase> extends DestroyableBase {
    /**
     * The method name
     */
    protected String name;

    /**
     * Is the method virtual or static
     */
    protected InvokeType invokeType;

    /**
     * The hosting class meta info
     */
    private T clsInfo;


}
