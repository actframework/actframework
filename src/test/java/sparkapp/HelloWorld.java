package sparkapp;

import org.osgl.http.H;
import org.osgl.util.S;

import static org.osgl.oms.boot.spark.SparkApp.*;
import static org.osgl.oms.controller.Controller.Util.*;

public class HelloWorld {
    public static void main(String[] args) {
        get("/hello", echo("hello world"));
    }
}
