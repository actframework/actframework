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

import com.alibaba.fastjson.JSON;
import org.osgl.exception.UnexpectedException;
import org.osgl.http.H;
import org.osgl.util.S;

import java.util.LinkedHashMap;

public class ResponseSpec implements InteractionPart {

    public H.Status status;
    public Object text;
    public LinkedHashMap<String, Object> html = new LinkedHashMap<>();
    public LinkedHashMap<String, Object> json = new LinkedHashMap<>();
    public LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
    public String checksum;
    public String downloadFilename;

    @Override
    public void validate(Interaction interaction) throws UnexpectedException {
        checkForEmpty(interaction);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    private void checkForEmpty(Interaction interaction) {
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
