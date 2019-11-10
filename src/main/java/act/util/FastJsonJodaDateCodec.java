package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
import org.osgl.$;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FastJsonJodaDateCodec extends LogSupportedDestroyableBase implements ObjectSerializer, ObjectDeserializer {

    private JodaDateTimeCodec dateTimeCodec;
    private JodaLocalDateCodec localDateCodec;
    private JodaLocalTimeCodec localTimeCodec;
    private JodaLocalDateTimeCodec localDateTimeCodec;

    private App app;

    @Inject
    public FastJsonJodaDateCodec(App app) {
        this.app = app;
    }

    @Override
    protected void releaseResources() {
        app = null;
        if (null != dateTimeCodec) {
            dateTimeCodec.destroy();
            dateTimeCodec = null;
        }
        if (null != localDateCodec) {
            localDateCodec.destroy();
            localDateCodec = null;
        }
        if (null != localTimeCodec) {
            localTimeCodec.destroy();
            localTimeCodec = null;
        }
        if (null != localDateTimeCodec) {
            localDateTimeCodec.destroy();
            localDateTimeCodec = null;
        }
        super.releaseResources();
    }

    public FastJsonJodaDateCodec(
            JodaDateTimeCodec dateTimeCodec,
            JodaLocalDateCodec localDateCodec,
            JodaLocalTimeCodec localTimeCodec,
            JodaLocalDateTimeCodec localDateTimeCodec
    ) {
        this.dateTimeCodec = dateTimeCodec;
        this.localDateCodec = localDateCodec;
        this.localTimeCodec = localTimeCodec;
        this.localDateTimeCodec = localDateTimeCodec;
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        int token = lexer.token();
        if (token == JSONToken.LITERAL_STRING) {
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
        } else if (token == JSONToken.LITERAL_INT) {
            long l = lexer.longValue();
            return (T) $.convert(l).to((Class)type);
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
            dateTimeCodec = app.getInstance(JodaDateTimeCodec.class);
        }
        return dateTimeCodec;
    }

    private JodaLocalDateTimeCodec localDateTimeCodec() {
        if (null == localDateTimeCodec) {
            localDateTimeCodec = app.getInstance(JodaLocalDateTimeCodec.class);
        }
        return localDateTimeCodec;
    }

    private JodaLocalDateCodec localDateCodec() {
        if (null == localDateCodec) {
            localDateCodec = app.getInstance(JodaLocalDateCodec.class);
        }
        return localDateCodec;
    }

    private JodaLocalTimeCodec localTimeCodec() {
        if (null == localTimeCodec) {
            localTimeCodec = app.getInstance(JodaLocalTimeCodec.class);
        }
        return localTimeCodec;
    }
}
