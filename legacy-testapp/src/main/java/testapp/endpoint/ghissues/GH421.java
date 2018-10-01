package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.GreetingService;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("421")
public class GH421 extends GithubIssueBase {

    @Configuration("greeting.service.cn.impl")
    private GreetingService cn;

    @Configuration("greeting.service.en.impl")
    private GreetingService en;

    @Configuration("cli.port")
    private int cliPort;

    @Configuration("foo.bar")
    public String fooBar;

    @GetAction("greeting/cn")
    public String testCn(String who) {
        return cn.greet(who);
    }

    @GetAction("greeting/en")
    public String testEn(String who) {
        return en.greet(who);
    }

    @GetAction("cliPort")
    public int cliPort() {
        return cliPort;
    }

    @GetAction("foo/bar")
    public String fooBar() {
        return fooBar;
    }

}
