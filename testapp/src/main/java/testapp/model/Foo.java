package testapp.model;

public class Foo {
    private String id;
    private Bar bar;
    private Bar[] barArray;

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
}
