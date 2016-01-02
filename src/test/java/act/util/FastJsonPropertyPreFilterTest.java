package act.util;

import act.TestBase;
import com.alibaba.fastjson.JSON;
import org.junit.Before;
import org.junit.Test;

public class FastJsonPropertyPreFilterTest extends TestBase {
    private FastJsonPropertyPreFilter filter;
    private Foo foo;

    @Before
    public void prepare() {
        filter = new FastJsonPropertyPreFilter();
        Zee zee = new Zee("zee", false);
        Bar bar = new Bar("bar", 5, zee);
        foo = new Foo("foo", bar);
    }

    @Test
    public void testIncludes() {
        filter.addIncludes("bar/zee/flag,bar.age,name");
        String s = JSON.toJSONString(foo, filter);
        eq("{\"bar\":{\"age\":5,\"zee\":{\"flag\":false}},\"name\":\"foo\"}", s);
    }

    @Test
    public void testExcludes() {
        filter.addExcludes("bar/zee/flag,name");
        String s = JSON.toJSONString(foo, filter);
        eq("{\"bar\":{\"age\":5,\"name\":\"bar\",\"zee\":{\"name\":\"zee\"}}}", s);
    }
}


class Zee {
    String name;
    boolean flag;
    public Zee(String s, boolean b) {
        name = s;
        flag = b;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}

class Bar {
    String name;
    int age;
    Zee zee;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    Bar(String name, int age, Zee zee) {
        this.name = name;
        this.age = age;
        this.zee = zee;
    }

    public Zee getZee() {
        return zee;
    }

    public void setZee(Zee zee) {
        this.zee = zee;
    }
}

class Foo {
    String name;
    Bar bar;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bar getBar() {
        return bar;
    }

    public void setBar(Bar bar) {
        this.bar = bar;
    }

    Foo(String name, Bar bar) {
        this.name = name;
        this.bar = bar;
    }
}