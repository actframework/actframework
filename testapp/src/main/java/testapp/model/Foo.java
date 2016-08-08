package testapp.model;

import java.util.List;

public class Foo {
    private String id;
    private Bar bar;
    private List<Bar> barArray;

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

    public List<Bar> getBarArray() {
        return barArray;
    }

    public void setBarArray(List<Bar> barArray) {
        this.barArray = barArray;
    }
}
