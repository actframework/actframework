package resourceloader;

import act.controller.annotation.UrlContext;
import act.data.annotation.Data;
import act.inject.util.LoadResource;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;
import java.util.Map;

@UrlContext("json")
public class JsonLoader {
    @Data
    public static class Character implements SimpleBean {
        public String username;
        public int level;
    }

    @LoadResource("characters.json")
    private List<Character> pojoList;

    @LoadResource("characters.json")
    private List<Map<String, Object>> mapList;

    @GetAction("pojo")
    public List<Character> getPojoList() {
        return pojoList;
    }

    @GetAction("map")
    public List<Map<String, Object>> getMapList() {
        return mapList;
    }
}
