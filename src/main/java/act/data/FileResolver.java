package act.data;

import java.io.File;

/**
 * Resolver File from path
 */
public class FileResolver extends StringValueResolverPlugin<File> {

    public static final FileResolver INSTANCE = new FileResolver();

    @Override
    public File resolve(String value) {
        return new File(value);
    }
}
