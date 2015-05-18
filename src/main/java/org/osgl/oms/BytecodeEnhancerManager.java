package org.osgl.oms;

import org.osgl._;
import org.osgl.oms.app.App;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.controller.bytecode.ControllerEnhancer;
import org.osgl.oms.util.AppBytecodeEnhancer;
import org.osgl.oms.util.AsmBytecodeEnhancer;
import org.osgl.oms.util.BytecodeVisitor;
import org.osgl.util.C;

import java.util.List;

public class BytecodeEnhancerManager {
    private List<AppBytecodeEnhancer> appEnhancers = C.newList();
    private List<AsmBytecodeEnhancer> generalEnhancers = C.newList();

    public BytecodeEnhancerManager() {
    }

    public void register(AsmBytecodeEnhancer enhancer) {
        if (enhancer instanceof AppBytecodeEnhancer) {
            appEnhancers.add((AppBytecodeEnhancer) enhancer);
        } else {
            generalEnhancers.add(enhancer);
        }
    }

    public void register(AppBytecodeEnhancer enhancer) {
        appEnhancers.add(enhancer);
    }

    public BytecodeVisitor appEnhancer(App app, String className, _.Var<ClassWriter> cw) {
        List<AppBytecodeEnhancer> l = appFilter(app, className);
        return l.isEmpty() ? null : BytecodeVisitor.chain(cw, l);
    }

    public BytecodeVisitor generalEnhancer(String className, _.Var<ClassWriter> cw) {
        List<AsmBytecodeEnhancer> l = generalFilter(className);
        return l.isEmpty() ? null : BytecodeVisitor.chain(cw, l);
    }

    private List<AppBytecodeEnhancer> appFilter(App app, String className) {
        List<AppBytecodeEnhancer> l = C.newList();
        for (AppBytecodeEnhancer e : appEnhancers) {
            AppBytecodeEnhancer e0 = (AppBytecodeEnhancer) e.clone();
            e0.app(app);
            if (e0.isTargetClass(className)) {
                l.add(e0);
            }
        }
        return l;
    }

    private List<AsmBytecodeEnhancer> generalFilter(String className) {
        List<AsmBytecodeEnhancer> l = C.newList();
        for (AsmBytecodeEnhancer e : generalEnhancers) {
            AsmBytecodeEnhancer e0 = e.clone();
            if (e0.isTargetClass(className)) {
                l.add(e0.clone());
            }
        }
        return l;
    }
}
