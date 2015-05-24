package act.view;

import org.osgl.http.H;
import act.app.AppContext;
import org.osgl.util.C;

import java.util.List;
import java.util.Map;

/**
 * Define system implicit variables
 */
public class SystemImplicitVariableProvider extends ImplicitVariableProvider {
    @Override
    public List<VarDef> implicitVariables() {
        return varDefs;
    }

    private List<VarDef> varDefs = C.listOf(
            new VarDef("_ctx", AppContext.class) {
                @Override
                public Object evaluate(AppContext context) {
                    return context;
                }
            },
//
//            new VarDef("_req", H.Request.class) {
//                @Override
//                public Object evaluate(AppContext context) {
//                    return context.req();
//                }
//            },
//
//            new VarDef("_resp", H.Response.class) {
//                @Override
//                public Object evaluate(AppContext context) {
//                    return context.resp();
//                }
//            },
//
            new VarDef("_session", H.Session.class) {
                @Override
                public Object evaluate(AppContext context) {
                    return context.session();
                }
            },

            new VarDef("_flash", H.Flash.class) {
                @Override
                public Object evaluate(AppContext context) {
                    return context.flash();
                }
            },

            new VarDef("_params", Map.class) {
                @Override
                public Object evaluate(AppContext context) {
                    return context.allParams();
                }
            }
    );
}
