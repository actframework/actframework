package resourceloader;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import act.util.JsonView;
import org.osgl.mvc.annotation.GetAction;

import java.util.Map;
import java.util.Properties;

@UrlContext("properties")
@JsonView
public class PropertiesLoader {

    @LoadResource("foo.properties")
    private Properties foo;

    @LoadResource("foo.properties")
    private Map<String, String> fooMap;

    @GetAction("foo")
    public Properties getFoo() {
        return foo;
    }

}
