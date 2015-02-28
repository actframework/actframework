package org.osgl.oms.app;

import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.route.Router;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.osgl.oms.controller.meta.ControllerClassMetaInfo.ACTION_METHODS;

/**
 * Scan srccode code for action method and build route table
 */
class SourceCodeActionScanner {

    private static final Logger logger = L.get(SourceCodeActionScanner.class);

    private static final Pattern PTN_ANN = Pattern.compile(
            "^\\s*@(Action|GetAction|PostAction|DeleteAction|PutAction).*");

    void scan(String className, String code, Router router) {
        String[] lines = code.split("[\\n\\r]+");
        int n = lines.length;
        for (int i = 0; i < n; ++i) {
            String line = lines[i];
            Matcher matcher = PTN_ANN.matcher(line);
            if (matcher.matches()) {
                RouteInfo ri = parseAnnotationLine(line, matcher.group(1));
                if (!ri.hasAction()) {
                    while (i < n - 1) {
                        line = lines[++i];
                        if (!isBlankOrCommentLine(line)) {
                            String action = parseMethodSignature(line);
                            if (null != action) {
                                StringBuilder sb = S.builder(className).append(".").append(action);
                                ri.action = sb.toString();
                                ri.addToRouter(router);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static final Pattern PTN_ANN_PARAM = Pattern.compile(
            "value\\s*=\\s*\"(.*)\"(\\s*,\\s*(methods\\s*=\\s*\\{(.*)\\})){0,1}");

    private static RouteInfo parseAnnotationLine(String line, String action) {
        FastStr fsLine = FastStr.of(line);
        H.Method[] methods = null;
        String path;
        if (!"Action".equals(action)) {
            H.Method method = H.Method.valueOf(S.before(action, "Action").toUpperCase());
            methods = new H.Method[]{method};
        }
        // filter out the annotation parameter part
        FastStr s = fsLine.afterFirst("Action").afterFirst('(').beforeFirst(')').trim();
        if (s.get(0) == '"') {
            path = s.substring(1, s.size() - 1); // strip the quotes
        } else {
            Matcher m = PTN_ANN_PARAM.matcher(s);
            if (m.matches()) {
                path = m.group(1);
                if (null == methods) {
                    String tmp = m.group(4);
                    if (S.isBlank(tmp)) {
                        methods = new H.Method[ACTION_METHODS.size()];
                        methods = ACTION_METHODS.toArray(methods);
                    } else {
                        String[] sa = tmp.split("[,\\s]+");
                        int n = sa.length;
                        methods = new H.Method[n];
                        for (int i = 0; i < n; ++i) {
                            String ms = sa[i];
                            if (S.isBlank(ms)) continue;
                            if (ms.startsWith("H")) {
                                ms = ms.replace("H.Method.", "");
                            }
                            methods[i] = H.Method.valueOfIgnoreCase(ms);
                        }
                    }
                }
            } else {
                logger.warn("Invalid action annotation found: %s", line);
                return null;
            }
        }

        s = fsLine.afterFirst(')').trim();
        if (isBlankOrCommentLine(s.toString())) {
            return new RouteInfo(path, methods);
        }
        action = parseMethodSignature(s.toString());
        return null == action ? null : new RouteInfo(path, action, methods);
    }

    private static String parseMethodSignature(String line) {
        String[] tokens = line.trim().replace("static ", " ").split("[\\s]+");
        if (tokens.length < 3 || !"public".equals(tokens[0])) {
            logger.warn("Invalid action method signature or non public action method: %s", line);
            return null;
        } else {
            return S.beforeFirst(tokens[2], "(");
        }
    }

    private static boolean isBlankOrCommentLine(String line) {
        return S.isBlank(line) || line.trim().startsWith("//");
    }

    private static class RouteInfo {
        String path;
        String action;
        H.Method[] methods;
        RouteInfo(String path, String action, H.Method[] methods) {
            this.path = path;
            this.action = action;
            this.methods = methods;
        }

        RouteInfo(String path, H.Method[] methods) {
            this.path = path;
            this.methods = methods;
        }

        boolean hasAction() {
            return null != action;
        }

        void addToRouter(Router router) {
            int n = methods.length;
            for (int i = 0; i < n; ++i) {
                router.addMappingIfNotMapped(methods[i], path, action);
            }
        }
    }
}
