package sparkapp;

import org.osgl.http.H;
import org.osgl.util.S;

import static org.osgl.oms.boot.SparkApp.*;
import static org.osgl.oms.controller.Controller.Util.*;

public class HelloWorld {
    public static void main(String[] args) {
        staticFileLocation("/public");
        get("/", echo("hello world"));
        get("/bye", new Handler() {
            @Override
            public void handle(H.Request req, H.Response resp) {
                String who = req.paramVal("who");
                notFoundIfNull(who, "para who expected");
                print(S.fmt("bye %s!", who));
            }
        });
    }
}
