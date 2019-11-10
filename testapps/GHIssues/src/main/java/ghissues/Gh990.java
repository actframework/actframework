package ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;
import javax.inject.Named;

@UrlContext("990")
public class Gh990 extends BaseController {

    @Inject
    @Named("a")
    private P p;

    @GetAction
    public String say(){
        return p.say();
    }

    public interface P {
        String say();
    }

    @Named("a")
    public static class A implements P{
        public String say(){
            return (this.getClass().getSimpleName());
        }
    }

    @Named("b")
    public static class B implements P{
        public String say(){
            return (this.getClass().getSimpleName());
        }
    }

    public static class Module extends org.osgl.inject.Module {
        @Override
        protected void configure() {
            bind(P.class).to(A.class).named("a");
        }
    }

}
