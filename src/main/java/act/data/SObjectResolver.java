package act.data;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.Codec;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Resolver String value into SObject
 */
public class SObjectResolver extends StringValueResolver<ISObject> {

    public static final SObjectResolver INSTANCE = new SObjectResolver();

    private OkHttpClient http;

    public SObjectResolver() {
        http = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS).build();
    }

    @Override
    public ISObject resolve(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return resolveFromURL(value);
        } else if (value.startsWith("data:")) {
            int pos = value.indexOf("base64,");
            if (pos < 0) {
                return null;
            }
            String encoded = value.substring(pos + "base64,".length());
            return resolveFromBase64(encoded);
        } else {
            File file = new File(value);
            if (file.exists() && file.canRead()) {
                return SObject.of(file);
            }
            // last try base64 decoder
            try {
                return resolveFromBase64(value);
            } catch (Exception e) {
                throw E.unexpected("Cannot resolve SObject from value: %s", value);
            }
        }
    }

    private ISObject resolveFromURL(String url) {
        try {
            Response resp = http.newCall(new Request.Builder().url(url).build()).execute();
            return SObject.of(resp.body().byteStream());
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    private ISObject resolveFromBase64(String encoded) {
        return SObject.of(Codec.decodeBase64(encoded));
    }

}
