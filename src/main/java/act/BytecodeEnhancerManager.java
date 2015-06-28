package act;

import act.app.App;
import act.asm.ClassWriter;
import act.util.AppByteCodeEnhancer;
import act.util.AsmByteCodeEnhancer;
import act.util.ByteCodeVisitor;
import org.osgl._;
import org.osgl.util.C;

import java.util.List;

public class BytecodeEnhancerManager {
    private List<AppByteCodeEnhancer> appEnhancers = C.newList();
    private List<AsmByteCodeEnhancer> generalEnhancers = C.newList();

    public BytecodeEnhancerManager() {
    }

    public void register(AsmByteCodeEnhancer enhancer) {
        if (enhancer instanceof AppByteCodeEnhancer) {
            appEnhancers.add((AppByteCodeEnhancer) enhancer);
        } else {
            generalEnhancers.add(enhancer);
        }
    }

    public void register(AppByteCodeEnhancer enhancer) {
        appEnhancers.add(enhancer);
    }

    public ByteCodeVisitor appEnhancer(App app, String className, _.Var<ClassWriter> cw) {
        List<AppByteCodeEnhancer> l = appFilter(app, className);
        return l.isEmpty() ? null : ByteCodeVisitor.chain(cw, l);
    }

    public ByteCodeVisitor generalEnhancer(String className, _.Var<ClassWriter> cw) {
        List<AsmByteCodeEnhancer> l = generalFilter(className);
        return l.isEmpty() ? null : ByteCodeVisitor.chain(cw, l);
    }

    private List<AppByteCodeEnhancer> appFilter(App app, String className) {
        List<AppByteCodeEnhancer> l = C.newList();
        for (AppByteCodeEnhancer e : appEnhancers) {
            AppByteCodeEnhancer e0 = (AppByteCodeEnhancer) e.clone();
            e0.app(app);
            if (e0.isTargetClass(className)) {
                l.add(e0);
            }
        }
        return l;
    }

    private List<AsmByteCodeEnhancer> generalFilter(String className) {
        List<AsmByteCodeEnhancer> l = C.newList();
        for (AsmByteCodeEnhancer e : generalEnhancers) {
            AsmByteCodeEnhancer e0 = e.clone();
            if (e0.isTargetClass(className)) {
                l.add(e0.clone());
            }
        }
        return l;
    }
}
