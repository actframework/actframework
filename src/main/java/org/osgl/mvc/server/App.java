package org.osgl.mvc.server;

import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.mvc.server.route.Router;
import org.osgl.util.C;

import java.util.List;
import java.util.Properties;

public class App {

    private static List<Class> actionAnnotationTypes = (List)C.list(Action.class, GetAction.class, PostAction.class, PutAction.class, DeleteAction.class);
    private static Router router = new Router(null, new AppConfig(new Properties())); static {
        router.addMapping(H.Method.GET, "/", "HomeController.index");
    }

    public static boolean isActionAnnotation(Class<?> type) {
        return actionAnnotationTypes.contains(type);
    }

    public static Router router() {
        return router;
    }
}
