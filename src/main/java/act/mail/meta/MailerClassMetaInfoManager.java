package act.mail.meta;

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

import static act.Destroyable.Util.destroyAll;

import act.asm.Type;
import act.util.LogSupportedDestroyableBase;

import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MailerClassMetaInfoManager extends LogSupportedDestroyableBase {

    private Map<String, MailerClassMetaInfo> mailers = new HashMap<>();

    public MailerClassMetaInfoManager() {
    }

    @Override
    protected void releaseResources() {
        destroyAll(mailers.values(), ApplicationScoped.class);
        mailers.clear();
        super.releaseResources();
    }

    public void registerMailerMetaInfo(MailerClassMetaInfo metaInfo) {
        String className = Type.getObjectType(metaInfo.className()).getClassName();
        mailers.put(className, metaInfo);
        trace("Mailer meta info registered for: %s", className);
    }

    public MailerClassMetaInfo mailerMetaInfo(String className) {
        return mailers.get(className);
    }

}
