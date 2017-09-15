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

import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;

public abstract class JodaReadableInstantCodecBase<T extends ReadableInstant> extends JodaDateTimeCodecBase<T> {

    public JodaReadableInstantCodecBase(DateTimeFormatter formatter) {
        super(formatter);
    }

    public JodaReadableInstantCodecBase(String pattern) {
        super(pattern);
    }

    @Override
    public String toString(T o) {
        return formatter().print(o);
    }
}
