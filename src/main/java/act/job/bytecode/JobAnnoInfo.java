package act.job.bytecode;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.app.event.AppEventId;
import act.util.AnnotationUtil;
import com.alibaba.fastjson.JSON;
import org.osgl.$;

import java.lang.annotation.Annotation;

public class JobAnnoInfo {
    public String value;
    public AppEventId appEventId;
    public boolean async;
    public String id;
    public Class<? extends Annotation> annotationType;

    JobAnnoInfo (Class <? extends Annotation> annoType) {
        this.annotationType = annoType;
        Object v = AnnotationUtil.tryGetDefaultValue(annoType, "value");
        if (v instanceof String) {
            this.value = (String) v;
        } else if (v instanceof AppEventId) {
            this.appEventId = (AppEventId) v;
        }
        this.async = $.bool((Boolean) AnnotationUtil.tryGetDefaultValue(annoType, "async"));
        this.id = (String) AnnotationUtil.tryGetDefaultValue(annoType, "id");
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
