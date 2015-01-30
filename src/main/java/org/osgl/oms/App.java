package org.osgl.oms;

import org.osgl.http.H;
import org.osgl.oms.route.Router;

import java.util.Properties;

public class App {


    private static Router router = new Router(null, new AppConfig(new Properties()));

    static {
        router.addMapping(H.Method.GET, "/", "HomeController.index");
    }

    public static Router router() {
        return router;
    }
}
