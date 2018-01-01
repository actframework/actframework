package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("325")
public class GH325 extends GithubIssueBase {

    @GetAction("data/{data}/key/{key}")
    public int test(Map<String, Integer> data, String key) {
        return data.get(key);
    }

    @GetAction("person/{name};{attributes}/{key}")
    public String test2(String name, Map<String, Integer> attributes, String key) {
        if (S.eq("name", key)) {
            return name;
        }
        return S.string(attributes.get(key));
    }

    @GetAction("list/{list}/{key}")
    public String test3(Map<String, List<Integer>> list, String key) {
        List<Integer> l = list.get(key);
        if (null == l) {
            return "0";
        }
        int n = 0;
        for (int x : l) {
            n += x;
        }
        return S.string(n);
    }

}
