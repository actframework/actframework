package ghissues;

import act.annotations.DownloadFilename;
import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.inject.param.NoBind;
import act.job.OnAppStart;
import act.util.LogSupport;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;

@UrlContext("829")
public class Gh829 extends LogSupport {

    public static class Foo {
        public String name = S.random();
    }

    @NoBind
    private List<Foo> foos;

    @OnAppStart
    public void prepareData() {
        foos = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            foos.add(new Foo());
        }
    }

    @GetAction
    @DownloadFilename("foo")
    public List<Foo> testStatic(ActionContext context, String nonce) {
        if (S.notBlank(nonce)) {
            context.downloadFileName("foo-" + nonce);
        }
        return foos;
    }

}
