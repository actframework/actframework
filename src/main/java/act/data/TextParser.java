package act.data;

import act.app.ActionContext;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TextParser extends RequestBodyParser {

    public static final TextParser INSTANCE = new TextParser();

    @Override
    public Map<String, String[]> parse(ActionContext context) {
        H.Request req = context.req();
        InputStream is = req.inputStream();
        try {
            Map<String, String[]> params = new HashMap<String, String[]>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            byte[] data = os.toByteArray();
            params.put(ActionContext.REQ_BODY, data.length == 0 ? null : new String[] {new String(data, req.characterEncoding())});
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
