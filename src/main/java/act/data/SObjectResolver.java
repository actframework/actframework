package act.data;

import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.Codec;
import org.osgl.util.StringValueResolver;

import java.io.File;

/**
 * Resolver String value into SObject
 */
public class SObjectResolver extends StringValueResolver<ISObject> {

    public static final SObjectResolver INSTANCE = new SObjectResolver();

    @Override
    public ISObject resolve(String value) {
        try {
            File file = new File(value);
            if (file.exists() && file.canRead()) {
                return SObject.of(file);
            }
        } catch (Exception e) {
            // ignore
        }
        String encoded = value;
        if (value.startsWith("data:")) {
            int pos = value.indexOf("base64,");
            if (pos < 0) {
                return null;
            }
            encoded = value.substring(pos + "base64,".length());
        }
        try {
            return SObject.of(Codec.decodeBase64(encoded));
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
