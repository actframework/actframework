package testapp.sbean;

import act.util.SimpleBean;

public interface SimpleModel extends SimpleBean {
    class SomeModel implements SimpleModel {
        public SomeModel(boolean flag) {}
    }
}
