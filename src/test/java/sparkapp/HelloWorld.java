package sparkapp;
import static act.boot.spark.SparkApp.*;

public class HelloWorld {
    public static void main(String[] args) {
        get("/hello", echo("hello world"));
    }
}
