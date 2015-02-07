package org.osgl.oms.plugin;

import org.osgl._;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.oms.OMS;
import org.osgl.oms.asm.ClassReader;
import org.osgl.oms.asm.ClassWriter;
import org.osgl.oms.cls.BootstrapClassLoader;
import org.osgl.oms.util.ClassDetector;
import org.osgl.util.E;

import java.util.Iterator;

/**
 * Responsible for scanning/loading OMS server plugins.
 * <p>A server plugin shall be packaged in jar file and put into
 * <code>${OMS_HOME}/plugin folder</code></p>
 */
public class PluginScanner {

    private static final Logger logger = L.get(PluginScanner.class);

    public PluginScanner() {
    }

    public void scan() {
        Iterator<byte[]> plugins = OMS.classLoader().pluginBytecodes();
        _.Visitor<byte[]> visitor = _F.BYTECODE_VISITOR;
        while (plugins.hasNext()) {
            byte[] ba = plugins.next();
            visitor.visit(ba);
        }
    }

    private static enum _F {
        ;
        static final _.Visitor<byte[]> BYTECODE_VISITOR =  new _.Visitor<byte[]>() {
            @Override
            public void visit(byte[] ba) throws _.Break {
                ClassReader cr = new ClassReader(ba);
                ClassWriter cw = new ClassWriter(0);
                ClassDetector detector = ClassDetector.chain(cw, Plugin.CLASS_FILTER);
                cr.accept(detector, 0);
                if (detector.found()) {
                    Class<Plugin> c;
                    String className = detector.className();
                    BootstrapClassLoader classLoader = OMS.classLoader();
                    try {
                        c = (Class<Plugin>) classLoader.createClass(className, ba);
                    } catch (LinkageError error) {
                        try {
                            c = (Class<Plugin>) classLoader.loadClass(className);
                        } catch (ClassNotFoundException e) {
                            throw E.unexpected(e);
                        }
                    }
                    Plugin.CLASS_FILTER.found(c);
                }
            }
        };
    }

}
