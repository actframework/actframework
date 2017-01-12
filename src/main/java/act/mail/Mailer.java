package act.mail;

import act.app.ActionContext;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
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

    /**
     * Defines the {@link MailerConfig mailer configure ID}.
     * <p>Default value: {@code "default"}</p>
     */
    String value() default "default";

    class Util {

        protected static final Logger logger = L.get(Mailer.class);

        public static Future<Boolean> send(Object... args) {
            throw E.unsupport("to be enhanced");
        }

        public static MailerContext mailer() {
            throw E.unsupport("to be enhanced");
        }

        public static void from(String from) {
            ctx().from = from;
        }

        public static void to(List<String> recipients) {
            ctx().to = S.join(",", recipients);
        }

        public static void to(String ... recipients) {
            ctx().to = S.join(",", recipients);
        }

        public static void cc(String ... cc) {
            ctx().cc = S.join(",", cc);
        }

        public static void bcc(String ... bcc) {
            ctx().bcc = S.join(",", bcc);
        }

        public static void subject(String subject, Object ... args) {
            ctx().subject = subject;
            ctx().subjectArgs = args;
        }

        private static SimpleContext ctx() {
            return _ctx.get();
        }

        public static Future<Boolean> doSendWithoutLoadThreadLocal(final MailerContext context) {
            return context.app().jobManager().now(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return context.send();
                }
            });
        }

        public static Future<Boolean> doSend(final MailerContext context) {
            tryLoadLocale(context);
            SimpleContext ctx = _ctx.get();
            if (null != ctx) {
                if (S.notBlank(ctx.from)) {
                    context.from(ctx.from);
                }
                if (S.notBlank(ctx.to)) {
                    context.to(ctx.to);
                }
                if (S.notBlank(ctx.cc)) {
                    context.cc(ctx.cc);
                }
                if (S.notBlank(ctx.bcc)) {
                    context.bcc(ctx.bcc);
                }
                if (S.notBlank(ctx.subject)) {
                    context.subject(ctx.subject, ctx.subjectArgs);
                }
                _ctx.remove();
            }
            return context.app().jobManager().now(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return context.send();
                }
            });
        }


        private static void tryLoadLocale(MailerContext context) {
            if (!context.config().i18nEnabled() || context.locale() != null) {
                // do nother if
                // 1. i18n is not enabled
                // 2. context locale has already loaded (via AppJobManager.ContextualJob)
                return;
            }
            // check if there are ActionContext instance in the render args
            for (Map.Entry<String, Object> entry : context.renderArgs().entrySet()) {
                Object val = entry.getValue();
                if (val instanceof ActionContext) {
                    // load locale from action context
                    context.locale(((ActionContext) val).locale());
                    return;
                }
            }
            // load locale from app config (default locale)
            context.locale(context.config().locale());
        }


        private static final ThreadLocal<SimpleContext> _ctx = new ThreadLocal<SimpleContext>() {
            @Override
            protected SimpleContext initialValue() {
                return new SimpleContext();
            }
        };

        private static class SimpleContext {
            String from;
            String to;
            String cc;
            String bcc;
            String subject;
            Object[] subjectArgs;
        }

    }
}
