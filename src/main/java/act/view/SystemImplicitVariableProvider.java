package act.view;

import act.ActComponent;
import act.app.ActionContext;
import act.mail.MailerContext;
import act.view.rythm.ActionViewVarDef;
import act.view.rythm.MailerViewVarDef;
import org.osgl.http.H;
import org.osgl.util.C;

import javax.mail.internet.InternetAddress;
import java.util.List;
import java.util.Map;

/**
 * Define system implicit variables
 */
@ActComponent
public class SystemImplicitVariableProvider extends ImplicitVariableProvider {
    @Override
    public List<ActionViewVarDef> implicitActionViewVariables() {
        return actionViewVarDefs;
    }

    @Override
    public List<MailerViewVarDef> implicitMailerViewVariables() {
        return mailerViewVarDefs;
    }

    private List<ActionViewVarDef> actionViewVarDefs = C.listOf(
            new ActionViewVarDef("_ctx", ActionContext.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context;
                }
            },
            new ActionViewVarDef("_session", H.Session.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context.session();
                }
            },
            new ActionViewVarDef("_flash", H.Flash.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context.flash();
                }
            },
            new ActionViewVarDef("_params", Map.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context.allParams();
                }
            }
    );

    private List<MailerViewVarDef> mailerViewVarDefs = C.listOf(
            new MailerViewVarDef("_mailer", MailerContext.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context;
                }
            },
            new MailerViewVarDef("_from", InternetAddress.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context.from();
                }
            },
            new MailerViewVarDef("_to", List.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context.to();
                }
            },
            new MailerViewVarDef("_cc", List.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context.cc();
                }
            },
            new MailerViewVarDef("_bcc", List.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context.bcc();
                }
            }
    );
}
