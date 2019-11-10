package gh935;

import act.Act;
import act.controller.annotation.UrlContext;
import act.data.annotation.DateFormatPattern;
import act.util.JsonView;
import org.osgl.mvc.annotation.GetAction;

import java.util.Date;

@UrlContext("935")
public class AppEntry {

    public static class Foo {
        public Date date;
        public String name;

        public Foo(Date date, String name) {
            this.date = date;
            this.name = name;
        }
    }

    @GetAction
    @JsonView
    public Foo test(@DateFormatPattern("yyyy-MM-dd") Date date, String name) {
        return new Foo(date, name);
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }
}
