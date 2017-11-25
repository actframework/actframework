package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

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

}
