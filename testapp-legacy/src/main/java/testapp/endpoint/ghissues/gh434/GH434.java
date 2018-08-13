package testapp.endpoint.ghissues.gh434;

import static org.osgl.util.E.illegalStateIf;

import act.controller.annotation.UrlContext;
import act.util.Stateless;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.ghissues.GithubIssueBase;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Test `@With` on action methods
 */
@UrlContext("434")
@Stateless
public class GH434 extends GithubIssueBase {

    @Configuration("gh434.by_lang")
    private Map<String, List<Service>> serviceByLang;

    @Inject
    private List<GreetingService> greetingServices;

    @Configuration("gh434.svs_list")
    private List<Service> configuredServices;

    @Configuration("gh434.setting")
    private Map<String, Boolean> settings;

    @GetAction("by_lang")
    public void testMap() {
        illegalStateIf(null == serviceByLang);
        illegalStateIf(serviceByLang.size() != 2);
        List<Service> cnServices = serviceByLang.get("cn");
        illegalStateIf(null == cnServices);
        illegalStateIf(cnServices.size() != 2);
        illegalStateIf(null == cnServices.get(0));
    }

    @GetAction("inject")
    public void testInject() {
        illegalStateIf(null == greetingServices);
        illegalStateIf(greetingServices.size() != 2);
        for (GreetingService svc : greetingServices) {
            illegalStateIf(null == svc);
        }
    }

    @GetAction("conf")
    public void testConfigured() {
        illegalStateIf(null == configuredServices);
        illegalStateIf(configuredServices.size() != 3);
        for (Service svc : configuredServices) {
            illegalStateIf(null == svc);
        }
    }

    @GetAction("setting")
    public void testSettings() {
        illegalStateIf(null == settings);
        illegalStateIf(settings.isEmpty());
        illegalStateIf(!settings.get("prod"));
        illegalStateIf(settings.get("debug"));
    }

}
