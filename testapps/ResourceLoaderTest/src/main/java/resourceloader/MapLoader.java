package resourceloader;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import org.osgl.mvc.annotation.GetAction;

import java.util.Map;
import javax.inject.Singleton;

@UrlContext("map")
@Singleton
public class MapLoader {

    @LoadResource("foo.properties")
    private Map<String, Object> generalMap;

    @LoadResource("int_values.properties")
    private Map<String, Integer> integerMap;

    @LoadResource("short_values.txt")
    private Map<String, Short> shortMap;

    @GetAction("general")
    public Map<String, Object> getGeneralMap() {
        return generalMap;
    }

    @GetAction("integer")
    public Map<String, Integer> getIntegerMap() {
        return integerMap;
    }

    @GetAction("short")
    public Map<String, Short> getShortMap() {
        return shortMap;
    }

}
