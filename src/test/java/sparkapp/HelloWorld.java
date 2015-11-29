package sparkapp;

import static act.boot.spark.SparkApp.echo;
import static act.boot.spark.SparkApp.get;

public class HelloWorld {
    public static void main(String[] args) {
        get("/hello", echo("hello world"));
    }
}
