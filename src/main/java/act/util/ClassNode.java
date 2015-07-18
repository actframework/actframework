package act.util;

import org.osgl._;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.reflect.Modifier;
import java.util.Set;

public class ClassNode {

    private ClassInfoRepository infoBase;
    private String name;
    private int modifiers;
    private ClassNode parent;
    private Set<ClassNode> children = C.newSet();
    private Set<ClassNode> interfaces = C.newSet();

    ClassNode(String name, int modifiers, ClassInfoRepository infoBase) {
        this(name, infoBase);
        this.modifiers = modifiers;
    }

    ClassNode(String name, ClassInfoRepository infoBase) {
        E.NPE(name, infoBase);
        this.name = name;
        this.infoBase = infoBase;
    }

    public String name() {
        return name;
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
        return Modifier.isAbstract(modifiers);
    }

    public boolean publicNotAbstract() {
        return isPublic() && !isAbstract();
    }

    public ClassNode parent(String name) {
        this.parent = infoBase.node(name);
        this.parent.addChild(this);
        return this;
    }

    public ClassNode addInterface(String name) {
        ClassNode intf = infoBase.node(name);
        this.interfaces.add(intf);
        intf.addChild(this);
        return this;
    }

    public ClassNode accept(_.Function<ClassNode, ?> treeVisitor) {
        for (ClassNode child : children) {
            child.accept(treeVisitor);
        }
        treeVisitor.apply(this);
        return this;
    }

    public ClassNode findPublicNotAbstract(_.Function<ClassNode, ?> treeVisitor) {
        return accept(_.guardedVisitor(new _.Predicate<ClassNode>() {
            @Override
            public boolean test(ClassNode classNode) {
                return classNode.publicNotAbstract();
            }
        }, treeVisitor));
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
            return _.eq(that.name, this.name);
        }
        return false;
    }

    private ClassNode addChild(ClassNode node) {
        children.add(node);
        return this;
    }
}
