package testapp.endpoint;

import act.controller.annotation.UrlContext;
import act.data.annotation.DateFormatPattern;
import com.alibaba.fastjson.JSON;
import org.joda.time.DateTime;
import org.osgl.mvc.annotation.GetAction;

import java.util.Arrays;
import java.util.Map;

@UrlContext("/bwa")
@SuppressWarnings("unused")
public class BindingWithAnnotationTestBed {

    @GetAction("datetime")
    public DateTime dateTime(@DateFormatPattern("yyyyMMdd") DateTime dateTime) {
        return dateTime;
    }

    public static void main(String[] args) {
        String str1="{\"v\": \"abc\"}";
        Map<String, char[]> map = JSON.parseObject(str1, new com.alibaba.fastjson.TypeReference<Map<String, char[]>>() {});
        char[] ca = map.get("v");
        System.out.println(Arrays.toString(ca));
    }

}
