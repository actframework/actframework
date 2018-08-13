package testapp.endpoint.ghissues.gh692;

import act.controller.annotation.UrlContext;
import act.util.Stateless;
import act.util.SubClassFinder;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.ghissues.GithubIssueBase;

import java.util.ArrayList;
import java.util.List;

public interface Gh692Service {
    String name();

    @UrlContext("692")
    @Stateless
    class Manager extends GithubIssueBase {
        private List<Gh692Service> serviceList = new ArrayList<>();

        @SubClassFinder
        public void foundService(Gh692Service svc) {
            serviceList.add(svc);
        }

        @GetAction
        public int count() {
            return serviceList.size();
        }
    }
}
