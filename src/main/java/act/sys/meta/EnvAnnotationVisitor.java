package act.sys.meta;

import act.Act;
import act.asm.AnnotationVisitor;
import act.asm.Opcodes;
import act.sys.Env;
import org.osgl.util.S;

import java.lang.annotation.Annotation;

/**
 * Scan `@Env.Mode`, `@Env.Profile`, `@Env.Group`
 */
public class EnvAnnotationVisitor extends AnnotationVisitor implements Opcodes {

    private boolean matched = true;
    private boolean unless = false;
    private Class<? extends Annotation> type;

    public EnvAnnotationVisitor(AnnotationVisitor annotationVisitor, Class<? extends Annotation> c) {
        super(ASM5, annotationVisitor);
        this.type = c;
    }

    @Override
    public void visit(String name, Object value) {
        if ("value".equals(name)) {
            String s = S.string(value);
            if (type == Env.Profile.class) {
                matched = Env.profileMatches(s);
            } else if (type == Env.Group.class) {
                matched = Env.groupMatches(s);
            }
        } else if ("unless".equals(name)) {
            unless = (Boolean) value;
        }
        super.visit(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        if ("value".equals(name) && desc.contains("Mode")) {
            Act.Mode mode = Act.Mode.valueOf(value);
            if (!Env.modeMatches(mode)) {
                matched = false;
            }
        }
        super.visitEnum(name, desc, value);
    }

    @Override
    public void visitEnd() {
        matched = unless ^ matched;
        super.visitEnd();
    }

    public boolean matched() {
        return matched;
    }

}
