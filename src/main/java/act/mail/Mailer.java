package act.mail;

import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;

import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * A Tag annotation indicate a class or method is used to preparing and send out email
 * <p>Note if the annotation is tagged on a class, then framework will treat all public
 * void method with name start with "send" as sender method</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Mailer {
    public static class Util {
        protected static final Logger logger = L.get(Mailer.class);
        public static void send(Object... args) {
            E.unsupport("to be enhanced");
        }

        public static Future<Boolean> doSend(final MailerContext context) {
            return context.app().jobManager().now(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        MimeMessage message = context.createMessage();
                        Transport.send(message);
                        return true;
                    } catch (Exception e) {
                        logger.error(e, "Error sending email: %s", context);
                        return false;
                    }
                }
            });
        }
    }
}
