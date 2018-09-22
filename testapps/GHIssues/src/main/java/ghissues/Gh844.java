package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import act.util.HeaderMapping;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;

@UrlContext("844")
public class Gh844 extends BaseController {

    public static class Country {
        public Integer no;
        public String code;
        public String name;
    }

    @LoadResource("countries.csv")
    @HeaderMapping("country as name")
    private List<Country> countryList;

    @GetAction
    public List<Country> test() {
        return countryList;
    }

}
