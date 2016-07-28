package act.mail.meta;

import act.asm.Type;
import act.util.DestroyableBase;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;

import static act.Destroyable.Util.destroyAll;

/**
 * Stores all class level information to support generating of mailer method
 */
@ApplicationScoped
public final class MailerClassMetaInfo extends DestroyableBase {

    private Type type;
    private String configId;
    private boolean isAbstract = false;
    private String ctxField = null;
    private boolean ctxFieldIsPrivate = true;
    private C.List<SenderMethodMetaInfo> senders = C.newList();
    // mailerLookup index mailer method by method name
    private C.Map<String, SenderMethodMetaInfo> mailerLookup = null;
    private boolean isMailer;
    private String contextPath;

    public MailerClassMetaInfo className(String name) {
        this.type = Type.getObjectType(name);
        return this;
    }

    @Override
    protected void releaseResources() {
        destroyAll(senders, ApplicationScoped.class);
        senders.clear();
        if (null != mailerLookup) {
            destroyAll(mailerLookup.values());
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
