package act.util;

import org.osgl.util.C;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The repository to keep class information
 */
public class ClassInfoRepository extends DestroyableBase {

    protected ConcurrentMap<String, ClassNode> classes = new ConcurrentHashMap<>();

    public boolean has(String className) {
        return classes.containsKey(className);
    }

    public ClassNode node(String name) {
        ClassNode node = classes.get(name);
        if (null == node) {
            ClassNode newNode = new ClassNode(name, this);
            node = classes.putIfAbsent(name, newNode);
            if (null == node) {
                node = newNode;
            }
        }
        return node;
    }

    @Override
    protected void releaseResources() {
        classes.clear();
    }

    public Map<String, ClassNode> classes() {
        return C.map(classes);
    }

    /**
     * Java {@code Class.getCanonicalName()} sometimes will throw out
     * {@code InternalError} with message: "{code Malformed class name}"
     * We just ingore it
     * @param c the class on which canonical name is returned
     * @return the canonical name of the class specified or {@code null} if no
     * canonical name found or error returned canonical name on the class
     */
    public static String canonicalName(Class c) {
        try {
            return c.getCanonicalName();
        } catch (InternalError e) {
            return null;
        }
    }
}
