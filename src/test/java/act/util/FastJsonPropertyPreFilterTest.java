package act.util;

import act.TestBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import org.junit.Before;
import org.junit.Test;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FastJsonPropertyPreFilterTest extends TestBase {
    private FastJsonPropertyPreFilter filter;
    private Foo foo;
    private Foo foo2;

    @Before
    public void prepare() {
        filter = new FastJsonPropertyPreFilter();
        Zee zee = new Zee("zee", false);
        Bar bar = new Bar("bar", 5, zee);
        foo = new Foo("foo", bar);
        foo2 = new Foo("foo2", bar);
        JsonUtilConfig.config();
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


    @Test
    public void testWithIterable() {
        class Person {
            private String name;
            public Person(String s) {
                this.name = s;
            }
            public String getName() {
                return name;
            }
        }
        final Person s1 = new Person("fast");
        final Person s2 = new Person("fast");
        Iterable<Person> iterable = new Iterable<Person>() {
            @Override
            public Iterator<Person> iterator() {
                return new Iterator<Person>() {
                    int cursor = 0;
                    @Override
                    public boolean hasNext() {
                        return cursor < 2;
                    }

                    @Override
                    public Person next() {
                        switch (cursor++) {
                            case 0:
                                return s1;
                            case 1:
                                return s2;
                            default:
                                throw new NoSuchElementException();
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        List<Person> list = new ArrayList<>();
        for (Person p : iterable) {
            list.add(p);
        }
        iterable = new FastJsonIterable<>(iterable);
        assertEquals("[{\"name\":\"fast\"},{\"name\":\"fast\"}]", JSON.toJSONString(list));
        assertEquals("[{\"name\":\"fast\"},{\"name\":\"fast\"}]", JSON.toJSONString(iterable));
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