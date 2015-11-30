package act.data;

import act.app.ActionContext;
import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.Map;

// Disclaim, major logic of this component come from PlayFramework 1.3's DataParser
public abstract class RequestBodyParser {

    protected static final Logger logger = L.get(RequestBodyParser.class);

    private static Map<H.Format, RequestBodyParser> parsers = C.newMap(
            H.Format.FORM_MULTIPART_DATA, new ApacheMultipartParser(),
            H.Format.FORM_URL_ENCODED, new UrlEncodedParser(),
            H.Format.JSON, TextParser.INSTANCE,
            H.Format.XML, TextParser.INSTANCE,
            H.Format.CSV, TextParser.INSTANCE
    );

    public static RequestBodyParser get(H.Request req) {
        H.Format contentType = req.contentType();
        RequestBodyParser parser = parsers.get(contentType);
        if (null == parser) {
            parser = TextParser.INSTANCE;
        }
        return parser;
    }

    public abstract Map<String, String[]> parse(ActionContext context);

}

