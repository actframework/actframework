package sparkapp;

public class HelloWorld {
    public static void main(String[] args) {
        get("/hello", echo("hello world"));
    }
}
