package act.view;

import act.mail.MailerContext;
import act.util.ActContext;
import act.view.VarDef;

public abstract class MailerViewVarDef extends VarDef {
    protected MailerViewVarDef(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    public final Object evaluate(ActContext context) {
        return eval((MailerContext) context);
    }

    public abstract Object eval(MailerContext context);
}
