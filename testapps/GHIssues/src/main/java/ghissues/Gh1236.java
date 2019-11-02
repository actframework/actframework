package ghissues;

import act.apidoc.SampleData;
import act.apidoc.SampleDataProvider;
import act.controller.ExpressController;
import act.controller.annotation.UrlContext;
import act.handler.NonBlock;
import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;

@UrlContext("1236")
@ExpressController
public class Gh1236 extends BaseController {

    private static class FooNameProvider extends SampleDataProvider<String> {
        @Override
        public String get() {
            return $.random("foo", "bar", "zee");
        }
    }

    public static class Foo {
        @SampleData.ProvidedBy(FooNameProvider.class)
        public String name;
    }

    /**
     * Return `Foo`
     */
    @GetAction
    public Foo test() {
        return new Foo();
    }

}
