package act.test;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.util.AdaptiveBeanBase;
import act.util.EnhancedAdaptiveMap;
import com.alibaba.fastjson.JSON;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResponseSpec extends AdaptiveBeanBase<ResponseSpec> implements InteractionPart {

    public enum Type {
        html, json, xml, header, headers
    }

    public H.Status status;
    public Object text;
    public LinkedHashMap<String, Object> html = new LinkedHashMap<>();
    public LinkedHashMap<String, Object> json = new LinkedHashMap<>();
    public LinkedHashMap<String, Object> xml = new LinkedHashMap<>();
    public LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
    public String checksum;
    public String downloadFilename;
    public Type __type;

    @Override
    public void validate(Interaction interaction) throws UnexpectedException {
        checkForEmpty(interaction);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    private void checkForEmpty(Interaction interaction) {
        if (size() == 0) {
            throw new UnexpectedException("Empty response spec found in interaction[%s]", interaction);
        }
        Map<String, Object> map = this.toMap();
        String accept;
        if (null != __type) {
            accept = __type.name();
        } else {
            RequestSpec req = interaction.request;
            accept = req.accept;
            if (null == accept) {
                accept = "json";
            } else {
                accept = accept.toLowerCase();
            }
        }
        List<Field> fields = $.fieldsOf(ResponseSpec.class, ResponseSpec.class, true, true);
        for (Field f : fields) {
            map.remove(f.getName());
        }
        if (accept.contains("json")) {
            json.putAll(map);
        } else if (accept.contains("html")) {
            html.putAll(map);
        } else if (accept.contains("xml")) {
            xml.putAll(map);
        } else if (accept.contains("header")) {
            headers.putAll(map);
        }
        if (null != status) {
            return;
        }
        if (null != text) {
            return;
        }
        if (!json.isEmpty()) {
            return;
        }
        if (!html.isEmpty()) {
            return;
        }
        if (!headers.isEmpty()) {
            return;
        }
        if (S.notBlank(checksum)) {
            return;
        }
        if (S.notBlank(downloadFilename)) {
            return;
        }
        throw new UnexpectedException("Empty response spec found in interaction[%s]", interaction);
    }

}
