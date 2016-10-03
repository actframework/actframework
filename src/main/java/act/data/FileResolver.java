package act.data;

import org.osgl.util.StringValueResolver;

import java.io.File;

/**
 * Resolver File from path
 */
public class FileResolver extends StringValueResolver<File> {

    public static final FileResolver INSTANCE = new FileResolver();

    @Override
    public File resolve(String value) {
        return new File(value);
    }
}
