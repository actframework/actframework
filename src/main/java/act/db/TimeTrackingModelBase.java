package act.db;

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

import act.data.Timestamped;
import act.inject.param.NoBind;
import org.osgl.$;

@NoBind
public abstract class TimeTrackingModelBase<
        ID_TYPE, MODEL_TYPE extends ModelBase,
        TIMESTAMP_TYPE, TIMESTAMP_TYPE_RESOLVER extends $.Function<TIMESTAMP_TYPE, Long>
        > extends ModelBase<ID_TYPE, MODEL_TYPE>
        implements TimeTrackingModel<TIMESTAMP_TYPE, TIMESTAMP_TYPE_RESOLVER>, Timestamped {
    @Override
    public boolean _isNew() {
        return null == _created();
    }

    @Override
    public long _timestamp() {
        return _timestampTypeResolver().apply(_lastModified());
    }
}
