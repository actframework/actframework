package testapp.endpoint;

import act.app.App;
import act.controller.annotation.UrlContext;
import act.util.InheritedStateless;
import act.util.Lazy;
import act.util.Stateful;
import act.util.Stateless;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

/**
 * Test if `@Stateless` and `@InheritedStateless` marked class
 * will be registered as a singleton
 */
@UrlContext("/stateless")
public class StatelessTestBed {

    private StatelessFoo foo;

    private StatelessBar bar;

    private StatefulBar barReverted;

    //@Stateless can't make the testbed stateless
    // otherwise we can't really test the stateless of Foo and Bar
    private final String id;

    private final Eager eager;

    private final LazyBoy lazyBoy;

    @Inject
    public StatelessTestBed(
            @NotNull App app,
            @NotNull StatelessFoo foo,
            @NotNull StatelessBar bar,
            @NotNull StatefulBar barReverted,
            @NotNull Eager eager,
            @NotNull LazyBoy lazyBoy
    ) {
        this.id = app.cuid();
        this.foo = foo;
        this.bar = bar;
        this.barReverted = barReverted;
        this.eager = eager;
        this.lazyBoy = lazyBoy;
    }

    public abstract static class StatelessBase {
        String id = S.random();
    }

    @Stateless
    public static class StatelessFoo extends StatelessBase {}

    @InheritedStateless
    public abstract static class InheritedStatelessBase extends StatelessBase {}

    public static class StatelessBar extends InheritedStatelessBase {}

    @Stateful
    public static class StatefulBar extends StatelessBar {}

    @Singleton
    public static class Eager extends StatelessBase {}

    @Singleton
    @Lazy
    public static class LazyBoy extends StatelessBase {}

    @GetAction
    public String id() {
        return id;
    }

    @GetAction("foo")
    public String foo() {
        return foo.id;
    }

    @GetAction("bar")
    public String bar() {
        return bar.id;
    }

    @GetAction("stopInheritedScope")
    public String barReverted() {
        return barReverted.id;
    }

    @GetAction("eager")
    public String eager() {
        return eager.id;
    }

    @GetAction("lazy")
    public String lazy() {
        return lazyBoy.id;
    }

}
