package ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.inject.param.NoBind;
import act.job.OnAppStart;
import act.util.AdaptiveBeanBase;
import act.util.PropertySpec;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.IO;
import org.osgl.util.S;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@UrlContext("1145")
@Singleton
public class Gh1145 extends BaseController {

    public static class Data extends AdaptiveBeanBase<Data> {
    }

    @NoBind
    public List<Data> list = new ArrayList<>();

    private String checksum;

    @OnAppStart
    public void generateRandomData() {
        // populate data list
        for (int i = 0; i < 10000; ++i) {
            Data data = new Data();
            for (int j = 0; j < 30; ++j) {
                String key = "key" + j;
                data.putValue(key, "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
            }
            list.add(data);
        }

        // try get csv file checksum
        String content = csvContent();
        checksum = IO.checksum(content.getBytes());
    }

    private String csvContent() {
        List<String> list = new ArrayList<>();
        for (int j = 0; j < 100; ++j) {
            list.add("key" + j);
        }
        String headline = S.join(",", list);
        List<String> dataLines = new ArrayList<>();
        dataLines.add(headline);
        for (Data data : this.list) {
            List<String> cell = new ArrayList<>();
            for (int j = 0; j < 100; ++j) {
                String key = "key" + j;
                cell.add((String) data.getValue(key));
            }
            dataLines.add(S.join(",", cell));
        }
        return S.join("\n", dataLines);
    }

    @GetAction("checksum")
    public String checksum() {
        return checksum;
    }

    @GetAction
    public List<Data> get(ActionContext context) {
        List<String> list = new ArrayList<>();
        for (int j = 0; j < 100; ++j) {
            list.add("key" + j);
        }
        String propSpec = S.join(",", list);
        PropertySpec.current.set(propSpec);
        return this.list;
    }

}
