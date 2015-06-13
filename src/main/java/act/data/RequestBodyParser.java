package act.data;

import act.app.AppContext;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.Map;

// Disclaim, major logic of this component come from PlayFramework 1.3's DataParser
public abstract class RequestBodyParser {

    protected static final Logger logger = L.get(RequestBodyParser.class);

    private static Map<H.Format, RequestBodyParser> parsers = C.newMap(
            H.Format.form_multipart_data, new ApacheMultipartParser(),
            H.Format.form_url_encoded, new UrlEncodedParser(),
            H.Format.json, TextParser.INSTANCE,
            H.Format.xml, TextParser.INSTANCE,
            H.Format.csv, TextParser.INSTANCE
    );

    public static RequestBodyParser get(H.Request req) {
        H.Format contentType = req.contentType();
        RequestBodyParser parser = parsers.get(contentType);
        if (null == parser) {
            parser = TextParser.INSTANCE;
        }
        return parser;
    }

    public abstract Map<String, String[]> parse(AppContext context);

}

