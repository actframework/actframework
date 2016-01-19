package act.util;

import act.app.App;
import act.data.JodaDateTimeCodec;
import act.data.JodaLocalDateCodec;
import act.data.JodaLocalDateTimeCodec;
import act.data.JodaLocalTimeCodec;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.joda.time.*;

import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;

@Singleton
public class FastJsonJodaDateCodec extends DestroyableBase implements ObjectSerializer, ObjectDeserializer {

    private JodaDateTimeCodec dateTimeCodec;
    private JodaLocalDateCodec localDateCodec;
    private JodaLocalTimeCodec localTimeCodec;
    private JodaLocalDateTimeCodec localDateTimeCodec;

    private App app;

    public FastJsonJodaDateCodec(App app) {
        this.app = app;
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        if (lexer.token() == JSONToken.LITERAL_STRING) {
            String text = lexer.stringVal();
            lexer.nextToken();

            if (type == DateTime.class) {
                DateTime dateTime = dateTimeCodec().parse(text);

                return (T) dateTime;
            } else if (type == LocalDateTime.class) {
                LocalDateTime localDateTime = localDateTimeCodec().parse(text);

                return (T) localDateTime;
            } else if (type == LocalDate.class) {
                LocalDate localDate = localDateCodec().parse(text);

                return (T) localDate;
            } else if (type == LocalTime.class) {
                LocalTime localDate = LocalTime.parse(text);

                return (T) localDate;
            } else if (type == Period.class) {
                Period period = Period.parse(text);

                return (T) period;
            } else if (type == Duration.class) {
                Duration duration = Duration.parse(text);

                return (T) duration;
            } else if (type == Instant.class) {
                Instant instant = Instant.parse(text);

                return (T) instant;
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return null;
    }

    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.getWriter();

        if (object == null) {
            out.writeNull();
            return;
        }

        Class cls = object.getClass();
        if (cls == DateTime.class) {
            out.writeString(dateTimeCodec().toString((DateTime) object));
            return;
        } else if (cls == LocalDateTime.class) {
            out.writeString(localDateTimeCodec().toString((LocalDateTime) object));
            return;
        } else if (cls == LocalDate.class) {
            out.writeString(localDateCodec().toString((LocalDate) object));
            return;
        } else if (cls == LocalTime.class) {
            out.writeString(localTimeCodec().toString((LocalTime) object));
            return;
        }

        out.writeString(object.toString());
    }

    private JodaDateTimeCodec dateTimeCodec() {
        if (null == dateTimeCodec) {
            dateTimeCodec = app.singleton(JodaDateTimeCodec.class);
        }
        return dateTimeCodec;
    }

    private JodaLocalDateTimeCodec localDateTimeCodec() {
        if (null == localDateTimeCodec) {
            localDateTimeCodec = app.singleton(JodaLocalDateTimeCodec.class);
        }
        return localDateTimeCodec;
    }

    private JodaLocalDateCodec localDateCodec() {
        if (null == localDateCodec) {
            localDateCodec = app.singleton(JodaLocalDateCodec.class);
        }
        return localDateCodec;
    }

    private JodaLocalTimeCodec localTimeCodec() {
        if (null == localTimeCodec) {
            localTimeCodec = app.singleton(JodaLocalTimeCodec.class);
        }
        return localTimeCodec;
    }
}
