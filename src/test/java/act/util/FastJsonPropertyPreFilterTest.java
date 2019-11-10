package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.ActTestBase;
import act.data.DataPropertyRepository;
import com.alibaba.fastjson.JSON;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FastJsonPropertyPreFilterTest extends ActTestBase {
    private FastJsonPropertyPreFilter filter;
    private Foo foo;
    private Foo foo2;
    private DataPropertyRepository repo;
    private List<S.Pair> fooProps;

    @BeforeClass
    public static void classInit() {
        FastJsonPropertyPreFilter.testClassInit();
    }

    @Before
    public void prepare() throws Exception {
        super.setup();
        filter = new FastJsonPropertyPreFilter();
        Zee zee = new Zee("zee", false);
        Zee zee2 = new Zee("zee2", true);
        Bar bar = new Bar("bar", 5, zee);
        Bar bar1 = new Bar("bar1", 4, zee2);
        Bar bar2 = new Bar("bar2", 3, null);
        foo = new Foo("foo", bar);
        foo2 = new Foo("foo2", bar, bar1, bar2);
        JsonUtilConfig.configure(mockApp);
        repo = new DataPropertyRepository(mockApp);
        fooProps = repo.propertyListOf(Foo.class);
        List<String> ls = new ArrayList<>();
        for (S.Pair pair : fooProps) {
            ls.add(pair._1);
        }
        filter.setFullPaths(ls);
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
        eq("{\"bar\":{\"age\":5,\"name\":\"bar\",\"zee\":{\"name\":\"zee\"}},\"barList\":[]}", s);
    }

    @Test
    public void testPatternIncludes() {
        filter.addIncludes(".*\\.flag");
        String s = JSON.toJSONString(foo2, filter);
        eq("{\"bar\":{\"zee\":{\"flag\":false}},\"barList\":[{\"zee\":{\"flag\":true}},{}]}", s);
    }

    @Test
    public void testPatternExclude() {
        filter.addExcludes("(.*\\.)?name");
        String s = JSON.toJSONString(foo2, filter);
        eq("{\"bar\":{\"age\":5,\"zee\":{\"flag\":false}},\"barList\":[{\"age\":4,\"zee\":{\"flag\":true}},{\"age\":3}]}", s);
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
        iterable = new FastJsonIterable(iterable);
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
    List<Bar> barList;

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

    public List<Bar> getBarList() {
        return barList;
    }

    public void setBarList(List<Bar> barList) {
        this.barList = barList;
    }

    Foo(String name, Bar bar, Bar ... bars) {
        this.name = name;
        this.bar = bar;
        this.barList = C.listOf(bars);
    }
}
