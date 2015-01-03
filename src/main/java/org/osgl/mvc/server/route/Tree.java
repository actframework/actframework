package org.osgl.mvc.server.route;

import org.osgl._;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.action.ActionInvoker;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.StrBase;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The data structure maintains routes mapping
 */
class Tree {
    static class Node implements Serializable {
        static final Node _root_() {
            return new Node(-1);
        }
        private int id;
        private StrBase name;
        private Pattern pattern;
        private CharSequence varName;
        private Node dynamicChild;
        private C.Map<CharSequence, Node> staticChildren = C.newMap();
        private ActionInvoker invoker;
        private Node(int id) {
            this.id = id;
            name = FastStr.EMPTY_STR;
        }
        Node(StrBase name) {
            E.NPE(name);
            this.name = name;
            this.id = name.hashCode();
            parseDynaName(name);
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof Node) {
                Node that = (Node)obj;
                return that.id == id && that.name.equals(name);
            }
            return false;
        }

        public boolean isDynamic() {
            return null != varName;
        }

        boolean metaInfoMatches(StrBase string) {
            _.T2<StrBase, Pattern> result = _parseDynaName(string);
            if (pattern != null && result._2 != null) {
                return pattern.pattern().equals(result._2.pattern());
            } else {
                // just allow route table to use different var names
                return true;
            }
        }

        public boolean matches(CharSequence chars) {
            if (!isDynamic()) return name.contentEquals(chars);
            if (null != pattern) {
                return pattern.matcher(chars).matches();
            }
            return true;
        }

        public Node child(CharSequence name, AppContext context) {
            Node node = staticChildren.get(name);
            if (null == node && null != dynamicChild) {
                if (dynamicChild.matches(name)) {
                    context.param(dynamicChild.varName.toString(), name.toString());
                    return dynamicChild;
                }
            }
            return node;
        }

        Node childByMetaInfo(StrBase s) {
            Node node = staticChildren.get(s);
            if (null == node && null != dynamicChild) {
                if (dynamicChild.metaInfoMatches(s)) {
                    return dynamicChild;
                }
            }
            return node;
        }

        Node addChild(StrBase<?> name) {
            name = name.trim();
            Node child = childByMetaInfo(name);
            if (null != child) {
                return child;
            }
            child = new Node(name);
            if (child.isDynamic()) {
                E.unexpectedIf(null != dynamicChild, "Cannot have more than one dynamic node in the route tree: %s", name);
                dynamicChild = child;
            } else {
                staticChildren.put(name, child);
            }
            return child;
        }

        void setInvoker(ActionInvoker invoker) {
            E.NPE(invoker);
            this.invoker = invoker;
        }

        private void parseDynaName(StrBase name) {
            _.T2<StrBase, Pattern> result = _parseDynaName(name);
            if (null != result) {
                this.varName = result._1;
                this.pattern = result._2;
            }
        }

        private static _.T2<StrBase, Pattern> _parseDynaName(StrBase name) {
            name = name.trim();
            if (name.startsWith("{") && name.endsWith("}")) {
                StrBase s = name.afterFirst('{').beforeLast('}').trim();
                if (s.contains('<') && s.contains('>')) {
                    StrBase varName = s.afterLast('>').trim();
                    StrBase ptn = s.afterFirst('<').beforeLast('>').trim();
                    Pattern pattern = Pattern.compile(ptn.toString());
                    return _.T2(varName, pattern);
                } else {
                    return _.T2(s, null);
                }
            }
            return null;
        }
    }

    private Node GET_ROOT = Node._root_();
    private Node POST_ROOT = Node._root_();
    private Node PUT_ROOT = Node._root_();
    private Node DEL_ROOT = Node._root_();

    public Node root(H.Method method) {
        switch (method) {
            case GET: return GET_ROOT;
            case POST: return POST_ROOT;
            case PUT: return PUT_ROOT;
            case DELETE: return DEL_ROOT;
            default:
                return null;
        }
    }

    private NotFound NOT_FOUND = new NotFound();

    public ActionInvoker getInvoker(H.Method method, List<CharSequence> path, AppContext context) {
        Node node = search(method, path, context);
        if (null == node) {
            throw NOT_FOUND;
        }
        ActionInvoker invoker = node.invoker;
        if (null == invoker) {
            throw NOT_FOUND;
        }
        return invoker;
    }

    private Node search(H.Method method, List<CharSequence> path, AppContext context) {
        Node node = root(method);
        int sz = path.size();
        int i = 0;
        while (null != node && i < sz) {
            CharSequence nodeName = path.get(i++);
            node = node.child(nodeName, context);
        }
        return node;
    }
}
