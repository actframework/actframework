package sparkapp;

import org.osgl.http.H;
import org.osgl.util.E;

import static act.boot.spark.SparkApp.*;
import static act.controller.Controller.Util.notFound;

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
            public Object handle(H.Request req, H.Response resp) {
                if (req.paramVal("foo") != null) {
                    notFound();
                }
                throw E.tbd();
            }
        });

        before("/protected(/.*|/?)", forbidden());
    }

}
