package act.data;

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

import act.conf.AppConfig;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class DateResolverBase<T extends Date> extends StringValueResolver<T> {
    static Logger logger = L.get(DateResolverBase.class);

    protected DateFormat dateFormat;
    protected DateFormat dateFormat2;

    public DateResolverBase(AppConfig config) {
        String pattern = configuredPattern(config);
        if (null == pattern || pattern.contains("8601") || pattern.contains("iso")) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        } else {
            dateFormat = new SimpleDateFormat(pattern);
        }
    }

    public DateResolverBase(String pattern) {
        dateFormat = new SimpleDateFormat(pattern);
    }

    @Override
    public final T resolve(String value) {
        if (S.blank(value)) {
            return null;
        }
        Date date = parse(value);
        if (null == date) {
            return null;
        }
        return cast(date);
    }

    protected String configuredPattern(AppConfig config) {
        return config.dateFormat();
    }

    protected abstract T cast(Date date);

    private Date parse(String value){
        try {
            return dateFormat.parse(value);
        } catch (ParseException e) {
            if (null == dateFormat2) {
                logger.error("error parsing date value from: %s", value);
                return null;
            }
            try {
                return dateFormat2.parse(value);
            } catch (ParseException e2) {
                logger.error("error parsing date value from: %s", value);
                return null;
            }
        }
    }


}
