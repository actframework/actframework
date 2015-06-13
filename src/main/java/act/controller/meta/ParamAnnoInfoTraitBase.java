package act.controller.meta;

import org.osgl.util.E;
import org.osgl.util.S;

abstract class ParamAnnoInfoTraitBase implements ParamAnnoInfoTrait {
    private int index;
    protected ParamAnnoInfoTraitBase(int index) {
        E.illegalArgumentIf(index < 0);
        this.index = index;
    }

    @Override
    public boolean compatibleWith(ParamAnnoInfoTrait otherParamAnnotation) {
        return otherParamAnnotation instanceof HarmonyParamAnnotationTraitBase;
    }

    @Override
    public String compatibilityErrorMessage(ParamAnnoInfoTrait otherParamAnnotation) {
        return S.fmt("Param annotations cannot co-exists: %s vs %s", getClass(), otherParamAnnotation.getClass());
    }

    @Override
    public int index() {
        return index;
    }
}
