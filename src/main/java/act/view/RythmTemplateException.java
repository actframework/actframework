package act.view;

import act.app.*;
import org.osgl.util.C;
import org.osgl.util.E;

public class RythmTemplateException extends TemplateException {

    public RythmTemplateException(org.rythmengine.exception.RythmException t) {
        super(t);
    }

    public org.rythmengine.exception.RythmException rythmException() {
        return (org.rythmengine.exception.RythmException) getCause();
    }

    @Override
    public String errorMessage() {
        Throwable t = super.getCause();
        if (t instanceof org.rythmengine.exception.RythmException) {
            return ((org.rythmengine.exception.RythmException) t).errorDesc();
        } else {
            return t.getMessage();
        }
    }

    @Override
    public boolean isErrorSpot(String traceLine, String nextTraceLine) {
        // if next trace line is rythm template build call, the this trace line
        // is the error spot
        /* Example
        at org.apache.commons.codec.binary.Hex.decodeHex(Hex.java:82)
        at org.osgl.util.Codec.hexStringToByte(Codec.java:190)
        at org.osgl.util.Crypto.decryptAES(Crypto.java:243)
        at act.app.util.AppCrypto.decrypt(AppCrypto.java:84)
        at act.fsa.views.ViewsDemo.backendServerError(ViewsDemo.java:26)
        at act.fsa.views.ViewsDemo.rt(ViewsDemo.java:80) <-- ERROR SPOT
        at act_fsa_views_ViewsDemo_rythmTemplateRuntimeError_html__R_T_C__.build(act_fsa_views_ViewsDemo_rythmTemplateRuntimeError_html__R_T_C__.java:163)
        at org.rythmengine.template.TemplateBase.__internalBuild(TemplateBase.java:740)
        at org.rythmengine.template.TemplateBase.__internalRender(TemplateBase.java:771)
         */
        return nextTraceLine.contains("__R_T_C__.build");
    }

    @Override
    protected boolean isTemplateEngineInvokeLine(String line) {
        throw E.unsupport();
    }

    @Override
    protected void populateSourceInfo(Throwable t) {
        org.rythmengine.exception.RythmException re = (org.rythmengine.exception.RythmException) t;
        sourceInfo = sourceInfo(re, true);
        templateInfo = sourceInfo(re, false);
    }

    private static SourceInfo sourceInfo(org.rythmengine.exception.RythmException e, boolean javaSource) {
        if (javaSource) {
            Throwable t = e.getCause();
            if (null != t && e.getClass().equals(org.rythmengine.exception.RythmException.class)) {
                SourceInfo javaSourceInfo = getJavaSourceInfo(t);
                if (null != javaSourceInfo) {
                    return javaSourceInfo;
                }
            }
            return new RythmSourceInfo(e, true);
        } else {
            return new RythmSourceInfo(e, false);
        }
    }

    private static class RythmSourceInfo extends SourceInfo.Base {

        RythmSourceInfo(org.rythmengine.exception.RythmException e, boolean javaSource) {
            fileName = e.templateName;
            if (javaSource) {
                lineNumber = e.javaLineNumber;
                String jsrc = e.javaSource;
                lines = null != jsrc ? C.listOf(jsrc.split("[\n]")) : C.<String>list();
            } else {
                lineNumber = e.templateLineNumber;
                lines = C.listOf(e.templateSource.split("[\n]"));
            }
        }
    }
}
