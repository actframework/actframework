package test;

import static act.controller.Controller.Util.render;

import act.Act;
import act.job.OnAppStart;
import org.joda.time.DateTime;
import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;

public class AppEntry {

    @GetAction("/test")
    public void test() {
        JiayouOrderTestEntity jiayouOrderTestEntity = new JiayouOrderTestEntity();
        jiayouOrderTestEntity.name = "123";
        jiayouOrderTestEntity.save();
        throw render(jiayouOrderTestEntity);
    }

    @OnAppStart
    public void doJob() {
        DateTime dateTime = $.convert("2018-11-13T13:28:01.952+1100").hint("iso").to(DateTime.class);
        System.out.println(dateTime);
    }

    public static void main(String[] args) throws Exception{
        Act.start();
    }
}
