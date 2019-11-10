package ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.data.annotation.Data;
import act.data.annotation.ParamBindingAnnotation;
import act.util.SimpleBean;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ValueLoader;
import org.osgl.inject.annotation.InjectTag;
import org.osgl.inject.annotation.LoadValue;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.E;

import java.lang.annotation.*;
import java.util.Map;

@UrlContext("1016")
public class Gh1016 extends BaseController {

    public static class IpLoader implements ValueLoader<String> {
        @Override
        public void init(Map<String, Object> options, BeanSpec spec) {
        }

        @Override
        public String get() {
            ActionContext context = ActionContext.current();
            E.illegalStateIf(null == context);
            return context.req().ip();
        }
    }


    @LoadValue(IpLoader.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @ParamBindingAnnotation
    @InjectTag
    public @interface FillIp {
    }

    @Data
    public static class Foo implements SimpleBean {
        public int id;
        @FillIp
        public String ip;
    }

    @Data
    public static class Bar implements SimpleBean {
        public Foo foo;
    }


    @PostAction
    public Foo create(Foo foo) {
        return foo;
    }

    @PostAction("embedded")
    public Bar createEmbedded(Bar bar) {
        return bar;
    }

}
