package testapp.endpoint.ghissues;

import act.Act;
import act.controller.annotation.UrlContext;
import act.inject.param.NoBind;
import org.osgl.cache.CacheService;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;
import javax.inject.Named;

@UrlContext("542")
public class GH542 extends GithubIssueBase {

    @Named("foo")
    @Inject
    private CacheService cache1;

    @NoBind
    private CacheService cache2 = Act.app().cache("foo");


    @GetAction
    public boolean test() {
        return cache1 == cache2;
    }

}
