package ghissues;

import act.controller.annotation.UrlContext;
import act.data.annotation.Data;
import act.inject.util.LoadResource;
import act.util.AdaptiveBean;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;

@UrlContext("1073")
public class Gh1073 extends BaseController {

    @Data
    public static class Config extends AdaptiveBean {

        @Data
        public static class Component implements SimpleBean {
            public int id;
            public String kind;
        }

        public String id;
        public String name;
        public List<Component> nameComponents;
    }

    @LoadResource("1073.yml")
    private List<Config> configs;

    @GetAction
    public List<Config> configs() {
        return configs;
    }

}
