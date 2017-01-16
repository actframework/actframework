package act.view;

import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.mail.MailerContext;
import act.security.CSRF;
import act.util.ActContext;
import org.osgl.http.H;
import org.osgl.util.C;
import org.rythmengine.utils.RawData;
import org.rythmengine.utils.S;

import javax.mail.internet.InternetAddress;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Define system implicit variables
 */
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
            new ActionViewVarDef("_app", App.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context.app();
                }
            },
            new ActionViewVarDef("_conf", AppConfig.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context.config();
                }
            },
            new ActionViewVarDef("_ctx", ActContext.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context;
                }
            },
            new ActionViewVarDef("_action", ActionContext.class) {
                @Override
                public Object eval(ActionContext context) {
                    return context;
                }
            },
            new ActionViewVarDef("_mailer", MailerContext.class) {
                @Override
                public Object eval(ActionContext context) {
                    return null;
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
            },
            new ActionViewVarDef("_csrf", String.class) {
                @Override
                public Object eval(ActionContext context) {
                    return CSRF.token(context);
                }
            },
            new ActionViewVarDef("_csrfField", RawData.class) {
                @Override
                public Object eval(ActionContext context) {
                    return S.raw(CSRF.formField(context));
                }
            },
            new ActionViewVarDef("_lang", String.class) {
                @Override
                public Object eval(ActionContext context) {
                    // TODO fix me
                    Locale locale = context.locale();
                    locale = Locale.getDefault();
                    return locale.toString().replace('_', '-');
                }
            }
    );

    private List<MailerViewVarDef> mailerViewVarDefs = C.listOf(
            new MailerViewVarDef("_app", App.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context.app();
                }
            },
            new MailerViewVarDef("_conf", AppConfig.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context.config();
                }
            },
            new MailerViewVarDef("_ctx", ActContext.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context;
                }
            },
            new MailerViewVarDef("_mailer", MailerContext.class) {
                @Override
                public Object eval(MailerContext context) {
                    return context;
                }
            },
            new MailerViewVarDef("_action", ActionContext.class) {
                @Override
                public Object eval(MailerContext context) {
                    return null;
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
