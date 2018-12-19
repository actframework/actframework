package ghissues;

import act.controller.annotation.UrlContext;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.storage.ISObject;
import org.osgl.util.StringValueResolver;

import java.util.List;
import java.util.Map;

@UrlContext("1027")
public class Gh1027 extends BaseController {

    public static class Foo implements SimpleBean {
        public String name;

        public Foo(String name) {
            this.name = name;
        }
    }

    public static class FooResolver extends StringValueResolver<Foo> {
        @Override
        public Foo resolve(String value) {
            return new Foo(value);
        }
    }

    @PostAction
    public Map<String, Foo> test(Map<String, Foo> data) {
        return data;
    }

    @PostAction("upload")
    public void testFile(List<ISObject> files) {
        System.out.println(files.size());
    }

}
