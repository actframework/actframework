package act;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.boot.server.ServerBootstrapClassLoader;
import act.util.ClassNames;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

public class TestServerBootstrapClassLoader extends ServerBootstrapClassLoader {

    public TestServerBootstrapClassLoader(ClassLoader cl) {
        super(cl);
    }

    protected byte[] tryLoadResource(String name) {
        if (!name.startsWith("act.")) return null;
        String fn = ClassNames.classNameToClassFileName(name, true);
        URL url = findResource(fn.substring(1));
        if (null == url) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IO.copy(url.openStream(), baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    @Override
    protected URL findResource(String name) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == this) {
            cl = Act.class.getClassLoader();
        }
        return cl.getResource(name);
    }
}
