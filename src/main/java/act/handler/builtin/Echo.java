package act.handler.builtin;

import act.app.ActionContext;
import act.handler.ExpressHandler;
import act.handler.builtin.controller.FastRequestHandler;
import org.osgl.http.H;
import org.osgl.util.Charsets;
import org.osgl.util.S;

import java.nio.ByteBuffer;

public class Echo extends FastRequestHandler implements ExpressHandler {

    private ByteBuffer buffer;
    private String toString;
    private String contentType;

    public Echo(String msg) {
        this(msg, H.Format.TXT.contentType());
    }

    public Echo(String msg, String contentType) {
        this.buffer = wrap(msg);
        this.contentType = contentType;
        this.toString = "echo: " + msg;
    }

    @Override
    public void handle(ActionContext context) {
        H.Response resp = context.resp();
        if (S.notBlank(contentType)) {
            resp.contentType(contentType);
        }
        resp.writeContent(buffer.duplicate());
    }

    public String readContent() {
        ByteBuffer copy = buffer.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return new String(bytes);
    }

    @Override
    public String toString() {
        return toString;
    }

    private ByteBuffer wrap(String content) {
        byte[] ba = content.getBytes(Charsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(ba.length);
        buffer.put(ba);
        buffer.flip();
        return buffer;
    }
}
