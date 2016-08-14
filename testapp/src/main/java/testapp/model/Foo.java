package testapp.model;

import java.util.List;
import java.util.Map;

public class Foo {

    public Foo() {
    }

    private String id;
    private Bar bar;
    private Bar[] barArray;
    private List<Integer> nl;
    private Map<String, Bar> barMap;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Bar getBar() {
        return bar;
    }

    public void setBar(Bar bar) {
        this.bar = bar;
    }

    public Bar[] getBarArray() {
        return barArray;
    }

    public void setBarArray(Bar[] barArray) {
        this.barArray = barArray;
    }

    public List<Integer> getNl() {
        return nl;
    }

    public void setNl(List<Integer> nl) {
        this.nl = nl;
    }

    public Map<String, Bar> getBarMap() {
        return barMap;
    }

    public void setBarMap(Map<String, Bar> barMap) {
        this.barMap = barMap;
    }
}
