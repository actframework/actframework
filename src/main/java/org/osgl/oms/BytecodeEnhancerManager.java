package org.osgl.oms;

import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.asm.ClassVisitor;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.util.AsmBytecodeEnhancer;
import org.osgl.oms.util.BytecodeVisitor;
import org.osgl.oms.util.PredicatableBytecodeVisitor;
import org.osgl.util.C;

import java.util.List;

public class BytecodeEnhancerManager {
    private List<AsmBytecodeEnhancer> appEnhancers = C.newList();
    private List<AsmBytecodeEnhancer> generalEnhancers = C.newList();

    public void register(AsmBytecodeEnhancer enhancer) {
        if (enhancer.isTargetClass(OMS.class.getName())) {
            generalEnhancers.add(enhancer);
        } else {
            appEnhancers.add(enhancer);
        }
    }

    public BytecodeVisitor appAsmEnhancer(String className, _.Var<ClassWriter> cw) {
        return buildAsmEnhancer(appEnhancers, className, cw);
    }

    public BytecodeVisitor generalAsmEnhancer(String className, _.Var<ClassWriter> cw) {
        return buildAsmEnhancer(generalEnhancers, className, cw);
    }

    private BytecodeVisitor buildAsmEnhancer(List<AsmBytecodeEnhancer> list, String className, _.Var<ClassWriter> cw) {
        List<AsmBytecodeEnhancer> filtered = filter(className, list);
        return filtered.isEmpty() ? null : BytecodeVisitor.chain(cw, filtered);
    }

    private List<AsmBytecodeEnhancer> filter(String className, List<AsmBytecodeEnhancer> list) {
        List<AsmBytecodeEnhancer> l = C.newList();
        for (AsmBytecodeEnhancer e : list) {
            if (e.isTargetClass(className)) {
                l.add(e);
            }
        }
        return l;
    }
}
