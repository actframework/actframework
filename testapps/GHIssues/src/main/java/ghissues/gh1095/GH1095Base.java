package ghissues.gh1095;

import act.util.SimpleBean;

import java.util.ArrayList;
import java.util.List;

public class GH1095Base<MODEL extends GH1095Base> implements SimpleBean {
    public List<MODEL> children = new ArrayList<>();
}
