package test;

import act.Act;
import act.util.PropertySpec;
import org.osgl.mvc.annotation.GetAction;

import java.util.ArrayList;
import java.util.List;

public class AppEntry {

    @GetAction("/query")
    @PropertySpec("mfield.test.a as 测试")
    public List<Bean> query() {
        List<Bean> beans = new ArrayList<>();
        beans.add(new Bean("a1"));
        beans.add(new Bean("a2"));
        return beans;
    }

    public static void main(String[] args) throws Exception{
        Act.start();
    }
}
