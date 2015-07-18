package act.view;

import act.app.ActionContext;
import act.mail.MailerContext;

/**
 * A Template represents a resource that can be merged with {@link ActionContext application context}
 * and output the result
 */
public interface Template {
    void merge(ActionContext context);

    String render(ActionContext context);

    String render(MailerContext context);
}
