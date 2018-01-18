package act.handler.event;

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

import act.event.ActEvent;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;

/**
 * This event will be triggered right before the {@link Result}
 * is committed
 */
public class AfterResultCommit extends ResultEvent {
    public AfterResultCommit(Result result, H.Request request, H.Response response) {
        super(result, request, response);
    }

    @Override
    public Class<? extends ActEvent<Result>> eventType() {
        return AfterResultCommit.class;
    }
}
