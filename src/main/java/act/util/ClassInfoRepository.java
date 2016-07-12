package act.util;

import act.Destroyable;
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
        String cname = canonicalName(name);
        ClassNode node = classes.get(cname);
        if (null == node) {
            ClassNode newNode = new ClassNode(name.replace('/', '.'), cname, this);
            node = classes.putIfAbsent(cname, newNode);
            if (null == node) {
                node = newNode;
            }
        }
        return node;
    }

    public ClassNode node(String name, String canonicalName) {
        String cname = canonicalName(name);
        ClassNode node = classes.get(name);
        if (null == node) {
            ClassNode newNode = new ClassNode(name.replace('/', '.'), canonicalName, this);
            node = classes.putIfAbsent(cname, newNode);
            if (null == node) {
                node = newNode;
            }
        }
        return node;
    }

    @Override
    protected void releaseResources() {
        Destroyable.Util.destroyAll(classes.values());
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
        } catch (IllegalAccessError e) {
            return null;
        }
    }

    public static String canonicalName(String name) {
        return name.replace('/', '.').replace('$', '.');
    }
}
