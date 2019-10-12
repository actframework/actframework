package ghissues;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import org.osgl.http.H;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

import static act.controller.Controller.Util.redirect;
import static act.controller.Controller.Util.renderTemplate;

@UrlContext("cookies")
public class DeleteCookieTest extends BaseController  {

    @GetAction
    public void home() {
        renderTemplate("/cookie_home.html");
    }

    @PostAction
    public void add(String cookieName, String cookieValue, ActionContext context) {
        H.Cookie cookie = new H.Cookie(cookieName, cookieValue).httpOnly(false);
        context.resp().addCookie(cookie);
        redirect("/cookies");
    }

    @DeleteAction
    public void delete(String cookieName, ActionContext context) {
        context.resp().removeCookie(cookieName);
        redirect("/cookies");
    }

}
