package act.util;

import act.app.*;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.util.List;

public interface ActError {
    Throwable getCauseOrThis();
    SourceInfo sourceInfo();
    List<String> stackTrace();
    boolean isErrorSpot(String traceLine, String nextTraceLine);

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

        public static SourceInfo loadSourceInfo(Method method) {
            DevModeClassLoader cl = (DevModeClassLoader) App.instance().classLoader();
            String className = method.getDeclaringClass().getName();
            Source source = cl.source(className);
            if (null == className) {
                return null;
            }
            List<String> lines = source.lines();
            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i);
                if (line.matches("^\\s*.*" + method.getName() + "\\s*\\(.*")) {
                    return new SourceInfoImpl(source, i + 1);
                }
            }
            return new SourceInfoImpl(source, 1);
        }
    }
}
