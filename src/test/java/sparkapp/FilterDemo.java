package sparkapp;

import org.osgl.http.H;
import org.osgl.oms.boot.spark.SparkApp;

import static org.osgl.oms.boot.spark.SparkApp.*;
import static org.osgl.oms.controller.Controller.Util.*;

/**
 * Show how to use Spark style filters
 */
public class FilterDemo {

    public static void main(String[] args) {
        get("/", echo("hello world"));
        get("/protected", echo("bye world"));
        get("/protected/secret", echo("Snowdern!"));

        before(new Handler() {
            @Override
            public void handle(H.Request req, H.Response resp) {
                if (req.paramVal("foo") != null) {
                    notFound();
                }
            }
        });

        before("/protected(/.*|/?)", SparkApp.forbidden());
    }

}
