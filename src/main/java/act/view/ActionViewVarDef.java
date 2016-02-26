package act.view;

import act.app.ActionContext;
import act.util.ActContext;
import act.view.VarDef;

public abstract class ActionViewVarDef extends VarDef {
    protected ActionViewVarDef(String name, Class<?> type) {
        super(name, type);
    }

    @Override
    public final Object evaluate(ActContext context) {
        return eval((ActionContext) context);
    }

    public abstract Object eval(ActionContext context);
}
