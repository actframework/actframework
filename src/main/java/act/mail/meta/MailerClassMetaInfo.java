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
import org.osgl.util.S;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

/**
 * Stores all class level information to support generating of mailer method
 */
@ApplicationScoped
public final class MailerClassMetaInfo extends LogSupportedDestroyableBase {

    private Type type;
    private String configId;
    private boolean isAbstract = false;
    private String ctxField = null;
    private boolean ctxFieldIsPrivate = true;
    private List<SenderMethodMetaInfo> senders = new ArrayList<>();
    // mailerLookup index mailer method by method name
    private Map<String, SenderMethodMetaInfo> mailerLookup = null;
    private boolean isMailer;
    private String contextPath;
    private String templateContext;

    public MailerClassMetaInfo className(String name) {
        this.type = Type.getObjectType(name);
        return this;
    }

    @Override
    protected void releaseResources() {
        destroyAll(senders, ApplicationScoped.class);
        senders.clear();
        if (null != mailerLookup) {
            destroyAll(mailerLookup.values(), ApplicationScoped.class);
            mailerLookup.clear();
        }
        super.releaseResources();
    }

    public String className() {
        return type.getClassName();
    }

    public MailerClassMetaInfo configId(String id) {
        configId = id;
        return this;
    }

    public String configId() {
        return configId;
    }

    public MailerClassMetaInfo templateContext(String templateContext) {
        this.templateContext = templateContext;
        return this;
    }

    public String templateContext() {
        return templateContext;
    }

    public String internalName() {
        return type.getInternalName();
    }

    public Type type() {
        return type;
    }

    public MailerClassMetaInfo setAbstract() {
        isAbstract = true;
        return this;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isMailer() {
        return isMailer;
    }

    public MailerClassMetaInfo isMailer(boolean b) {
        isMailer = b;
        return this;
    }

    public MailerClassMetaInfo ctxField(String fieldName, boolean isPrivate) {
        ctxField = fieldName;
        ctxFieldIsPrivate = isPrivate;
        return this;
    }

    public String nonPrivateCtxField() {
        if (null != ctxField) {
            return ctxFieldIsPrivate ? null : ctxField;
        }
        return null;
    }

    public String ctxField() {
        if (null != ctxField) {
            return ctxField;
        }
        return null;
    }

    public boolean hasCtxField() {
        return null != ctxField;
    }

    public boolean ctxFieldIsPrivate() {
        return ctxFieldIsPrivate;
    }

    public MailerClassMetaInfo addSender(SenderMethodMetaInfo info) {
        senders.add(info);
        return this;
    }

    public SenderMethodMetaInfo sender(String name) {
        if (null == mailerLookup) {
            for (SenderMethodMetaInfo act : senders) {
                if (S.eq(name, act.name())) {
                    return act;
                }
            }
            return null;
        }
        return mailerLookup.get(name);
    }

    public String contextPath() {
        return contextPath;
    }

    public MailerClassMetaInfo contextPath(String path) {
        if (S.blank(path)) {
            contextPath = "/";
        } else {
            contextPath = path;
        }
        return this;
    }


}
