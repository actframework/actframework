/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.osgl.mvc.server.asm.optimizer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * A MAPPING from names to names, used to rename classes, fields and methods.
 * 
 * @author Eric Bruneton
 */
public class NameMapping {

    public final Properties mapping;

    public final Set<Object> unused;

    public NameMapping(final String file) throws IOException {
        mapping = new Properties();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            mapping.load(is);
            unused = new HashSet<Object>(mapping.keySet());
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String map(final String name) {
        String s = (String) mapping.get(name);
        if (s == null) {
            int p = name.indexOf('.');
            if (p == -1) {
                s = name;
            } else {
                int q = name.indexOf('(');
                if (q == -1) {
                    s = name.substring(p + 1);
                } else {
                    s = name.substring(p + 1, q);
                }
            }
        } else {
            unused.remove(name);
        }
        return s;
    }

    public String fix(final String desc) {
        if (desc.startsWith("(")) {
            org.osgl.mvc.server.asm.Type[] arguments = org.osgl.mvc.server.asm.Type.getArgumentTypes(desc);
            org.osgl.mvc.server.asm.Type result = org.osgl.mvc.server.asm.Type.getReturnType(desc);
            for (int i = 0; i < arguments.length; ++i) {
                arguments[i] = fix(arguments[i]);
            }
            result = fix(result);
            return org.osgl.mvc.server.asm.Type.getMethodDescriptor(result, arguments);
        } else {
            return fix(org.osgl.mvc.server.asm.Type.getType(desc)).getDescriptor();
        }
    }

    private org.osgl.mvc.server.asm.Type fix(final org.osgl.mvc.server.asm.Type t) {
        if (t.getSort() == org.osgl.mvc.server.asm.Type.OBJECT) {
            return org.osgl.mvc.server.asm.Type.getObjectType(map(t.getInternalName()));
        } else if (t.getSort() == org.osgl.mvc.server.asm.Type.ARRAY) {
            String s = fix(t.getElementType()).getDescriptor();
            for (int i = 0; i < t.getDimensions(); ++i) {
                s = '[' + s;
            }
            return org.osgl.mvc.server.asm.Type.getType(s);
        } else {
            return t;
        }
    }
}
