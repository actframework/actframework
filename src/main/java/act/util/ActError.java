package act.util;

import act.app.*;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.List;

public interface ActError {
    Throwable getCauseOrThis();
    SourceInfo sourceInfo();
    List<String> stackTrace();

    class Util {
        public static List<String> stackTraceOf(Throwable t, ActError root) {
            List<String> l = C.newList();
            while (null != t) {
                StackTraceElement[] a = t.getStackTrace();
                for (StackTraceElement e : a) {
                    l.add("at " + e.toString());
                }
                t = t.getCause();
                if (t == root) {
                    break;
                }
                if (null != t) {
                    l.add("Caused by " + t.toString());
                }
            }
            return l;
        }

        public static SourceInfo loadSourceInfo(StackTraceElement[] stackTraceElements, Class<? extends ActError> errClz) {
            DevModeClassLoader cl = (DevModeClassLoader) App.instance().classLoader();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                int line = stackTraceElement.getLineNumber();
                if (line <= 0) {
                    continue;
                }
                String className = stackTraceElement.getClassName();
                if (S.eq(errClz.getName(), className)) {
                    continue;
                }
                Source source = cl.source(className);
                if (null == source) {
                    continue;
                }
                return new SourceInfoImpl(source, line);
            }
            return null;
        }
    }
}
