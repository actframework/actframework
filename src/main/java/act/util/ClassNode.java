package act.util;

import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClassNode extends DestroyableBase {

    private transient ClassInfoRepository infoBase;
    private String name;
    private String canonicalName;
    private int modifiers;
    private ClassNode parent;
    private transient Set<ClassNode> children = C.newSet();
    Map<String, ClassNode> interfaces = new HashMap<>();
    Set<ClassNode> annotations = C.newSet();
    Set<ClassNode> annotated = C.newSet();

    ClassNode(String name, int modifiers, ClassInfoRepository infoBase) {
        this(name, name.replace('$', '.'), modifiers, infoBase);
    }

    ClassNode(String name, String canonicalName, int modifiers, ClassInfoRepository infoBase) {
        this(name, canonicalName, infoBase);
        this.modifiers = modifiers;
    }

    ClassNode(String name, ClassInfoRepository infoBase) {
        this(name, name.replace('$', '.'), infoBase);
    }

    ClassNode(String name, String canonicalName, ClassInfoRepository infoBase) {
        E.NPE(name, infoBase);
        this.name = name;
        this.canonicalName = canonicalName;
        this.infoBase = infoBase;
    }

    public String name() {
        return name;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public ClassNode modifiers(int modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public int modifiers() {
        return modifiers;
    }

    public boolean isPublic() {
        return Modifier.isPublic(modifiers);
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers);
    }

    public boolean publicNotAbstract() {
        return isPublic() && !isAbstract();
    }

    public ClassNode parent() {
        return parent;
    }

    /**
     * Specify the class represented by this `ClassNode` extends
     * a class with the name specified
     * @param name the name of the parent class
     * @return this `ClassNode` instance
     */
    public ClassNode parent(String name) {
        this.parent = infoBase.node(name);
        this.parent.addChild(this);
        for (ClassNode intf : parent.interfaces.values()) {
            addInterface(intf);
        }
        return this;
    }

    /**
     * Specify the class represented by this `ClassNode` implements
     * an interface specified by the given name
     *
     * @param name the name of the interface class
     * @return this `ClassNode` instance
     */
    public ClassNode addInterface(String name) {
        ClassNode intf = infoBase.node(name);
        addInterface(intf);
        return this;
    }

    void addInterface(ClassNode classNode) {
        this.interfaces.put(classNode.name(), classNode);
        classNode.addChild(this);
        for (ClassNode child : children) {
            child.addInterface(classNode);
        }
    }

    public boolean hasInterface(String name) {
        return interfaces.containsKey(name);
    }

    /**
     * Specify the class represented by this `ClassNode` is annotated
     * by an annotation class specified by the name
     * @param name the name of the annotation class
     * @return this `ClassNode` instance
     */
    public ClassNode annotatedWith(String name) {
        ClassNode anno = infoBase.node(name);
        this.annotations.add(anno);
        anno.annotated.add(this);
        return this;
    }

    void addAnnotation(ClassNode classNode) {
        this.annotations.add(classNode);
    }

    void addAnnontated(ClassNode classNode) {
        this.annotated.add(classNode);
    }

    /**
     * Accept a visitor that visit all descendants of the class represented
     * by this `ClassNode` including this `ClassNode` itself
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitTree($.Function<ClassNode, ?> visitor) {
        visitSubTree(visitor);
        visitor.apply(this);
        return this;
    }

    private static $.Predicate<ClassNode> classNodeFilter(final boolean publicOnly, final boolean noAbstract) {
        return new $.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                if (publicOnly && !classNode.isPublic()) {
                    return false;
                }
                if (noAbstract && classNode.isAbstract()) {
                    return false;
                }
                return true;
            }
        };
    }

    /**
     * Accept a visitor that visit all descendants of the class represetned by
     * this `ClassNode` including this `ClassNode` itself.
     * @param visitor the visitor
     * @param publicOnly specify if only public class shall be visited
     * @param noAbstract specify if abstract class can be visited
     * @return this `ClassNode` instance
     */
    public ClassNode visitTree($.Function<ClassNode, ?> visitor, final boolean publicOnly, final boolean noAbstract) {
        return visitTree($.guardedVisitor(classNodeFilter(publicOnly, noAbstract), visitor));
    }

    /**
     * Accept a visitor that visit all descendants of the class represented
     * by this `ClassNode` NOT including this `ClassNode` itself
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitSubTree($.Function<ClassNode, ?> visitor) {
        for (ClassNode child : children) {
            child.visitTree(visitor);
        }
        return this;
    }

    /**
     * Accept a visitor that visit all descendants of the class represetned by
     * this `ClassNode` NOT including this `ClassNode` itself.
     * @param visitor the visitor
     * @param publicOnly specify if only public class shall be visited
     * @param noAbstract specify if abstract class can be visited
     * @return this `ClassNode` instance
     */
    public ClassNode visitSubTree($.Function<ClassNode, ?> visitor, final boolean publicOnly, final boolean noAbstract) {
        return visitSubTree($.guardedVisitor(classNodeFilter(publicOnly, noAbstract), visitor));
    }

    /**
     * Accept a visitor that visit all public and non-abstract descendants of the
     * class represented by this `ClassNode` including this `ClassNode` itself
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitPublicTreeNodes($.Function<ClassNode, ?> visitor) {
        return visitTree($.guardedVisitor(new $.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                return classNode.isPublic();
            }
        }, visitor));
    }

    /**
     * Accept a visitor that visit all public and non-abstract descendants of the
     * class represented by this `ClassNode` NOT including this `ClassNode` itself
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitPublicSubTreeNodes($.Function<ClassNode, ?> visitor) {
        return visitSubTree($.guardedVisitor(new $.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                return classNode.isPublic();
            }
        }, visitor));
    }

    /**
     * Accept a visitor that visit all public descendants of the
     * class represented by this `ClassNode` NOT including this `ClassNode` itself
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitPublicNotAbstractSubTreeNodes($.Function<ClassNode, ?> visitor) {
        return visitSubTree($.guardedVisitor(new $.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                return classNode.publicNotAbstract();
            }
        }, visitor));
    }

    /**
     * Accept a visitor that visit all public descendants of the
     * class represented by this `ClassNode` including this `ClassNode` itself
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitPublicNotAbstractTreeNodes($.Function<ClassNode, ?> visitor) {
        return visitTree($.guardedVisitor(new $.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                return classNode.publicNotAbstract();
            }
        }, visitor));
    }

    /**
     * Returns a set of `ClassNode` that has been annotated by the annotation
     * class represented by this `ClassNode`
     * @return the annotated class node set
     */
    public Set<ClassNode> annotatedClasses() {
        return C.set(annotated);
    }

    /**
     * Accept a visitor that visit all class node that has been annotated by the
     * class represented by this `ClassNode`
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitAnnotatedClasses($.Function<ClassNode, ?> visitor) {
        for (ClassNode annotated : this.annotated) {
            visitor.apply(annotated);
        }
        return this;
    }

    /**
     * Accept a visitor that visit all class node that has been annotated by the
     * class represented by this `ClassNode`
     * @param visitor the function that take `ClassNode` as argument
     * @param publicOnly specify whether non-public class shall be scanned
     * @param noAbstract specify whether abstract class shall be scanned
     * @return this `ClassNode` instance
     */
    public ClassNode visitAnnotatedClasses($.Function<ClassNode, ?> visitor, boolean publicOnly, boolean noAbstract) {
        return visitAnnotatedClasses($.guardedVisitor(classNodeFilter(publicOnly, noAbstract), visitor));
    }

    /**
     * Accept a visitor that visit all public class node
     * that has been annotated by the class represented by this `ClassNode`
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitPublicAnnotatedClasses($.Function<ClassNode, ?> visitor) {
        return visitAnnotatedClasses($.guardedVisitor(new $.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                return classNode.isPublic();
            }
        }, visitor));
    }

    /**
     * Accept a visitor that visit all public and non-abstract class node
     * that has been annotated by the class represented by this `ClassNode`
     * @param visitor the function that take `ClassNode` as argument
     * @return this `ClassNode` instance
     */
    public ClassNode visitPublicNotAbstractAnnotatedClasses($.Function<ClassNode, ?> visitor) {
        return visitAnnotatedClasses($.guardedVisitor(new $.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                return classNode.publicNotAbstract();
            }
        }, visitor));
    }

    /**
     * Returns a set of class node that annotated the class represented by this
     * `ClassNode`
     * @return the annotation class node set
     */
    public Set<ClassNode> annotations() {
        return C.set(annotations);
    }

    @Override
    protected void releaseResources() {
        children.clear();
        interfaces.clear();
        annotated.clear();
        annotations.clear();
        infoBase = null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ClassNode) {
            ClassNode that = (ClassNode) obj;
            return $.eq(that.name, this.name);
        }
        return false;
    }

    ClassNodeDTO toDTO() {
        return new ClassNodeDTO(this);
    }

    ClassNode addChild(ClassNode node) {
        children.add(node);
        for (ClassNode intf : interfaces.values()) {
            node.addInterface(intf);
        }
        return this;
    }
}
