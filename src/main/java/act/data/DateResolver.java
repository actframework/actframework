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
import act.data.annotation.DateFormatPattern;
import act.data.annotation.Pattern;
import org.osgl.util.AnnotationAware;
import org.osgl.util.StringValueResolver;

import java.util.Date;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DateResolver extends DateResolverBase<Date> {

    @Inject
    public DateResolver(AppConfig config) {
        super(config);
    }

    public DateResolver(String pattern) {
        super(pattern);
    }

    @Override
    public StringValueResolver<Date> amended(AnnotationAware beanSpec) {
        DateFormatPattern dfp = beanSpec.getAnnotation(DateFormatPattern.class);
        if (null != dfp) {
            return new DateResolver(dfp.value());
        }
        String format;
        DateFormatPattern pattern = beanSpec.getAnnotation(DateFormatPattern.class);
        if (null == pattern) {
            Pattern patternLegacy = beanSpec.getAnnotation(Pattern.class);
            format = null == patternLegacy ? null : patternLegacy.value();
        } else {
            format = pattern.value();
        }
        return null == format ? this : new DateResolver(format);

    }

    @Override
    protected Date cast(Date date) {
        return date;
    }
}
