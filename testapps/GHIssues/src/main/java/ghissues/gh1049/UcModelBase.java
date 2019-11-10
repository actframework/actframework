package ghissues.gh1049;

import act.util.AdaptiveBean;

public class UcModelBase extends AdaptiveBean {
    @Override
    public int hashCode() {
        return asMap().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return false;
        }
        if (obj.getClass().equals(getClass())) {
            return ((UcModelBase) obj).asMap().equals(asMap());
        }
        return false;
    }
}
