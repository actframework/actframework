package act.view;

import act.app.SourceInfo;
import org.osgl.util.C;

import java.util.List;

public class RythmException extends TemplateException {

    public RythmException(org.rythmengine.exception.RythmException t) {
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
    protected void populateSourceInfo(Throwable t) {
        org.rythmengine.exception.RythmException re = (org.rythmengine.exception.RythmException) t;
        sourceInfo = new RythmSourceInfo(re, true);
        templateInfo = new RythmSourceInfo(re, false);
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
