package ghissues;

import act.apidoc.SampleData;
import act.apidoc.SampleDataProvider;
import act.controller.ExpressController;
import act.controller.annotation.UrlContext;
import act.handler.NonBlock;
import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.S;

import javax.inject.Provider;

/**
 * Note there is no automate test for this issue.
 *
 * One must go to http://localhost:15460/~/api to check if the API
 * sample data has been generated correctly
 */
@UrlContext("1236")
public class Gh1236 extends BaseController {

    private static class FooNameProvider implements Provider<String> {
        @Override
        public String get() {
            return $.random("foo", "bar", "zee");
        }
    }

    public static class Foo {
        @SampleData.ProvidedBy(FooNameProvider.class)
        public String name;

        @SampleData.StringList({"a", "b", "c"})
        public String s;

        @SampleData.IntList({1, 2, 3})
        public int i;

        @SampleData.DoubleList({13.0d, 23.7d, 37.3d})
        public double d;
    }

    /**
     * Return `Foo`
     */
    @PostAction
    public Foo test(
            Foo foo,
            @SampleData.ProvidedBy(FooNameProvider.class) String bar,
            @SampleData.IntList({1, 2, 3, 5, 8}) int n
    ) {
        return foo;
    }

}
