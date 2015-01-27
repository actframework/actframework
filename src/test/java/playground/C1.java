package playground;

import org.osgl._;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Ok;
import org.osgl.mvc.result.Result;
import org.osgl.mvc.server.AppConfig;
import org.osgl.mvc.server.AppContext;
import org.osgl.util.S;

public class C1 extends CBase {

    private static boolean cond1() {
        return _.random(true, false);
    }

    @Action(value = "/", methods = {})
    public void root(String id, String email, boolean b) {
        String name = "root";
        if (b) {
            int i = 5;
            render(id, email, name, b, i);
            //System.out.println("abc");
        } else {
            if (S.empty(id)) {
                int i = 0;
                throw renderStatic("abc.html", id, i);
            } else if (S.empty(email)) {
                notFound("not found: %s", 404);
            } else {
                String reason = "abc";
                int code = 5, code2 = 3, code4 = 2;
                badRequest(reason, -1, code, code2, code4);
            }
        }
    }
//
//    @GetAction("/do_anno")
//    private void doAnno(@Param("svc_id") String svcId, int age, @Param("map") String map) {
//        ok();
//    }
//
//    @GetAction("/foo")
//    private static void bar(String id, String email, AppContext ctx) {
//        if (cond1()) {
//            renderStatic(id, email, ctx);
//        }
//        ok();
//    }

    @GetAction("/doIt")
    public static void doIt(@Param("acc_id") String id, String email, boolean b, AppContext ctx) {
        int i = 0, j = 1;
        if (b) {
            ok();
        }
        renderStatic("", i, j, b, id, email);
    }
//
//    @Action(value = "/foo/bar", methods = {H.Method.POST, H.Method.PUT})
//    public Result bar(String x) {
//        return ok();
//    }
//
//    public void echo() {
//        System.out.println("C1 processed");
//    }
}
