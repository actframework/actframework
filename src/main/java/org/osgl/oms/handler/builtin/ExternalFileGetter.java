package org.osgl.oms.handler.builtin;

import org.osgl.mvc.result.NotFound;

import java.io.*;

/**
 * Kind of {@link StaticFileGetter} but read file from external file system
 * e.g. /var/www/html/index.html etc, instead of from the JVM class path
 */
public class ExternalFileGetter extends StaticFileGetter {
    public ExternalFileGetter(String base) {
        super(base);
    }

    public ExternalFileGetter(String base, boolean baseIsFile) {
        super(base, baseIsFile);
    }

    @Override
    protected InputStream inputStream(String path) {
        File f = new File(path);
        if (f.exists()) {
            if (f.canRead() && f.isFile()) {
                try {
                    return new BufferedInputStream(new FileInputStream(f));
                } catch (IOException e) {
                    throw new NotFound();
                }
            }
        }
        throw new NotFound();
    }
}
