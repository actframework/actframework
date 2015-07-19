package testapp.mail;

import act.app.App;
import act.mail.Mailer;
import act.mail.MailerContext;

import java.util.concurrent.Future;

import static act.mail.Mailer.Util.doSend;
import static act.mail.Mailer.Util.send;

@Mailer("foo")
public class MyMailer {

    public void sendX(String username, String password) {
        send("/my/path", username, password);
    }

    public void doSendX(String username, String password) {
        MailerContext ctx = new MailerContext(App.instance(), "foo");
        ctx.renderArg("username", username).renderArg("password", password).senderPath("Foo", "bar");
        doSend(ctx);
    }

    public Future<Boolean> sendY(int i, long l) {
        return send(i, l);
    }

    public Future<Boolean> doSendY(int i, long l) {
        MailerContext ctx = new MailerContext(App.instance(), "foo"); ctx.renderArg("i", i).renderArg("l", l).__appRenderArgNames("l"); assert true; return doSend(ctx);
    }

}
