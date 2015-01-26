package playground;

import org.osgl.mvc.server.asm.ClassReader;
import org.osgl.mvc.server.asm.MethodVisitor;
import org.osgl.mvc.server.asm.Opcodes;
import org.osgl.mvc.server.asm.tree.*;
import org.osgl.mvc.server.asm.tree.analysis.*;
import org.osgl.mvc.server.asm.util.Textifier;
import org.osgl.mvc.server.asm.util.TraceMethodVisitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Eric Bruneton
 */
public class Analysis implements Opcodes {

    public static void main(final String[] args) throws Exception {
        ClassReader cr = new ClassReader("playground.Analysis");
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG);

        List<MethodNode> methods = cn.methods;
        for (int i = 0; i < methods.size(); ++i) {
            MethodNode method = methods.get(i);
            if (method.instructions.size() > 0) {
                if (!analyze(cn, method)) {
                    Analyzer<?> a = new Analyzer<BasicValue>(
                            new BasicVerifier());
                    try {
                        a.analyze(cn.name, method);
                    } catch (Exception ignored) {
                    }
                    final Frame<?>[] frames = a.getFrames();

                    Textifier t = new Textifier(ASM5) {
                        @Override
                        public void visitMaxs(final int maxStack,
                                final int maxLocals) {
                            for (int i = 0; i < text.size(); ++i) {
                                StringBuffer s = new StringBuffer(
                                        frames[i] == null ? "null"
                                                : frames[i].toString());
                                while (s.length() < Math.max(20, maxStack
                                        + maxLocals + 1)) {
                                    s.append(' ');
                                }
                                System.err.print(Integer.toString(i + 1000)
                                        .substring(1)
                                        + " "
                                        + s
                                        + " : "
                                        + text.get(i));
                            }
                            System.err.println();
                        }
                    };
                    MethodVisitor mv = new TraceMethodVisitor(t);
                    for (int j = 0; j < method.instructions.size(); ++j) {
                        Object insn = method.instructions.get(j);
                        ((AbstractInsnNode) insn).accept(mv);
                    }
                    mv.visitMaxs(0, 0);
                }
            }
        }
    }

    /*
     * Detects unused xSTORE instructions, i.e. xSTORE instructions without at
     * least one xLOAD corresponding instruction in their successor instructions
     * (in the control flow graph).
     */
    public static boolean analyze(final ClassNode c, final MethodNode m)
            throws Exception {
        Analyzer<SourceValue> a = new Analyzer<SourceValue>(
                new SourceInterpreter());
        Frame<SourceValue>[] frames = a.analyze(c.name, m);

        // for each xLOAD instruction, we find the xSTORE instructions that can
        // produce the value loaded by this instruction, and we put them in
        // 'stores'
        Set<AbstractInsnNode> stores = new HashSet<AbstractInsnNode>();
        for (int i = 0; i < m.instructions.size(); ++i) {
            AbstractInsnNode insn = m.instructions.get(i);
            int opcode = insn.getOpcode();
            if ((opcode >= ILOAD && opcode <= ALOAD) || opcode == IINC) {
                int var = opcode == IINC ? ((IincInsnNode) insn).var
                        : ((VarInsnNode) insn).var;
                Frame<SourceValue> f = frames[i];
                if (f != null) {
                    Set<AbstractInsnNode> s = f.getLocal(var).insns;
                    Iterator<AbstractInsnNode> j = s.iterator();
                    while (j.hasNext()) {
                        insn = j.next();
                        if (insn instanceof VarInsnNode) {
                            stores.add(insn);
                        }
                    }
                }
            }
        }

        // we then find all the xSTORE instructions that are not in 'stores'
        boolean ok = true;
        for (int i = 0; i < m.instructions.size(); ++i) {
            AbstractInsnNode insn = m.instructions.get(i);
            int opcode = insn.getOpcode();
            if (opcode >= ISTORE && opcode <= ASTORE) {
                if (!stores.contains(insn)) {
                    ok = false;
                    System.err.println("method " + m.name + ", instruction "
                            + i + ": useless store instruction");
                }
            }
        }
        return ok;
    }

    /*
     * Test for the above method, with three useless xSTORE instructions.
     */
    public int test(int i, int j) {
        i = i + 1; // ok, because i can be read after this point

        if (j == 0) {
            j = 1; // useless
        } else {
            try {
                j = j - 1; // ok, because j can be accessed in the catch
                int k = 0;
                if (i > 0) {
                    k = i - 1;
                }
                return k;
            } catch (Exception e) { // useless ASTORE (e is never used)
                j = j + 1; // useless
            }
        }

        return 0;
    }
}
