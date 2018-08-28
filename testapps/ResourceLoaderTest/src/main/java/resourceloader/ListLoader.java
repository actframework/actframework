package resourceloader;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;
import javax.inject.Singleton;

@UrlContext("list")
@Singleton
public class ListLoader {

    @LoadResource("int.list")
    private List<Integer> intList;

    @GetAction("int")
    public List<Integer> getIntList() {
        return intList;
    }

}
