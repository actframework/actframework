package testapp.endpoint.ghissues.gh426;

import act.controller.annotation.UrlContext;
import act.util.FastJsonFeature;
import act.util.FastJsonFilter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.PascalNameFilter;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.ghissues.GithubIssueBase;

@UrlContext("426")
public class GH426 extends GithubIssueBase {

    public static class Foo {
        public String name;
        public int BarCount;
        public Boolean flag;

        public Foo(String name, int barCount) {
            this.name = name;
            BarCount = barCount;
        }
    }

    @GetAction
    @FastJsonFilter(PascalNameFilter.class)
    @FastJsonFeature({SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.PrettyFormat})
    public Foo foo(String name, int count) {
        return new Foo(name, count);
    }

    public static void main(String[] args) {
        SerializeConfig.getGlobalInstance().setAsmEnable(false);
        Foo foo = new Foo("Name", 5);
        String json = JSON.toJSONString(foo, new PascalNameFilter(), SerializerFeature.WriteNullBooleanAsFalse);
        System.out.println(json);
    }
}
