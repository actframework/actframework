package act.mail;

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

import act.Act;
import act.app.ActionContext;
import act.app.App;
import act.test.Test;
import act.event.ActEvent;
import act.event.SystemEvent;
import act.util.ActContext;
import act.view.Template;
import act.view.ViewManager;
import org.osgl.$;
import org.osgl.concurrent.ContextLocal;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.*;

import java.io.File;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class MailerContext extends ActContext.Base<MailerContext> {

    public static class InitEvent extends ActEvent<MailerContext> implements SystemEvent {
        public InitEvent(MailerContext source) {
            super(source);
        }

        @Override
        public Class<? extends ActEvent<MailerContext>> eventType() {
            return InitEvent.class;
        }
    }

    private static final Logger logger = LogManager.get(MailerContext.class);

    private H.Format fmt = H.Format.HTML;
    private InternetAddress from;
    private String subject;
    private String content;
    private List<InternetAddress> to = new ArrayList<>();
    private List<InternetAddress> cc = new ArrayList<>();
    private List<InternetAddress> bcc = new ArrayList<>();
    private String confId;
    private List<ISObject> attachments = new ArrayList<>();
    private String senderPath; // e.g. com.mycorp.myapp.mailer.AbcMailer.foo

    private static final ContextLocal<MailerContext> _local = $.contextLocal();

    public MailerContext(App app, String confId) {
        this(app, confId, null);
    }

    public MailerContext(App app, String confId, String templateContext) {
        super(app);
        this.confId = confId;
        _local.set(this);
        ActionContext actionContext = ActionContext.current();
        if (null != actionContext) {
            locale(actionContext.locale());
        }
        app.eventBus().triggerSync(new InitEvent(this));
        super.templateContext(templateContext);
    }

    @Override
    protected void releaseResources() {
        super.releaseResources();
        to.clear();
        cc.clear();
        bcc.clear();
        _local.remove();
    }

    public MailerContext configId(String id) {
        confId = id;
        return this;
    }

    public String senderPath() {
        return senderPath;
    }

    public MailerContext senderPath(String path) {
        senderPath = path;
        return this;
    }

    @Override
    public String methodPath() {
        return senderPath;
    }

    public MailerContext senderPath(String className, String methodName) {
        senderPath = S.concat(className, ".", methodName);
        return this;
    }

    public MailerConfig mailerConfig() {
        return app().mailerConfigManager().config(confId);
    }

    @Override
    public Set<String> paramKeys() {
        throw E.tbd();
    }

    @Override
    public String paramVal(String key) {
        throw E.tbd();
    }

    @Override
    public String[] paramVals(String key) {
        throw E.tbd();
    }

    @Override
    public MailerContext accept(H.Format fmt) {
        E.NPE(fmt);
        this.fmt = fmt;
        return this;
    }

    @Override
    public H.Format accept() {
        return null != fmt ? fmt : mailerConfig().contentType();
    }

    @Override
    public MailerContext templatePath(String templatePath) {
        return super.templatePath(templatePath);
    }

    @Override
    public MailerContext templateLiteral(String literal) {
        // Need to declare this method for bytecode enhancement
        return super.templateLiteral(literal);
    }

    @Override
    public <T> T renderArg(String name) {
        return super.renderArg(name);
    }

    @Override
    public MailerContext renderArg(String name, Object val) {
        return super.renderArg(name, val);
    }

    @Override
    public Map<String, Object> renderArgs() {
        return super.renderArgs();
    }

    /**
     * Called by bytecode enhancer to set the name list of the render arguments that is update
     * by the enhancer
     * @param names the render argument names separated by ","
     * @return this AppContext
     */
    public MailerContext __appRenderArgNames(String names) {
        return super.__appRenderArgNames(names);
    }

    public H.Format contentType() {
        return accept();
    }

    public String subject() {
        return null != subject ? subject : mailerConfig().subject();
    }

    public MailerContext subject(String subject, Object ... args) {
        String emailId = Test.generateEmailId();
        if (null != emailId) {
            emailId = "[test-" + emailId + "]";
        }
        if (S.blank(subject)) {
            if (null != emailId) {
                this.subject = emailId;
            }
            return this;
        }
        if (app().config().i18nEnabled()) {
            this.subject = i18n(subject, args);
        } else {
            this.subject = S.fmt(subject, args);
        }
        if (null != emailId) {
            this.subject = emailId + this.subject;
        }
        return this;
    }

    public MailerContext content(String content, Object ... args) {
        if (null != content) {
            this.content = S.fmt(content, args);
        } else {
            this.content = null;
        }
        return this;
    }

    public MailerContext attach(ISObject... attachments) {
        this.attachments.addAll(C.listOf(attachments));
        return this;
    }

    public MailerContext attach(Collection<ISObject> attachments) {
        this.attachments.addAll(attachments);
        return this;
    }

    public MailerContext attach(File... files) {
        for (File file : files) {
            attachments.add(SObject.of(file));
        }
        return this;
    }

    public MailerContext from(String from) {
        E.illegalArgumentIf(S.empty(from), "<from> cannot be empty");
        List<InternetAddress> l = canonicalRecipients(null, from);
        E.illegalArgumentIf(l.isEmpty(), "from address expected");
        if (l.size() > 1) {
            logger.warn("There are more than one email address specified, only the first one will be used as From address");
        }
        this.from = l.get(0);
        return this;
    }

    public InternetAddress from() {
        if (null == from) {
            return mailerConfig().from();
        }
        return from;
    }

    /**
     * Set to recipients
     *
     * @param recipients the list of emails
     * @return this mailer context
     */
    public MailerContext to(String... recipients) {
        to = canonicalRecipients(null, recipients);
        return this;
    }

    public List<InternetAddress> to() {
        return to.isEmpty() ? mailerConfig().to() : to;
    }

    public MailerContext cc(String... recipients) {
        cc = canonicalRecipients(null, recipients);
        return this;
    }

    public List<InternetAddress> cc() {
        return cc.isEmpty() ? mailerConfig().ccList() : cc;
    }

    public MailerContext bcc(String... recipients) {
        bcc = canonicalRecipients(null, recipients);
        return this;
    }

    public List<InternetAddress> bcc() {
        return bcc.isEmpty() ? mailerConfig().bccList() : bcc;
    }

    public MailerContext addTo(String... recipients) {
        canonicalRecipients(to, recipients);
        return this;
    }

    public MailerContext addCc(String... recipients) {
        canonicalRecipients(cc, recipients);
        return this;
    }

    public MailerContext addBcc(String... recipients) {
        canonicalRecipients(bcc, recipients);
        return this;
    }

    public boolean send() {
        try {
            MimeMessage message = createMessage();
            if (!mailerConfig().mock()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending email\n%sEnd email\n", debug(message));
                }
                Transport.send(message);
            } else {
                logger.info("Sending email\n%sEnd email\n", debug(message));
            }
            return true;
        } catch (Exception e) {
            logger.error(e, "Error sending email: %s", this);
            return false;
        }
    }

    private String debug(MimeMessage msg) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n>> recipients");
        Address[] aa = msg.getAllRecipients();
        if (null != aa) {
            for (Address a: aa) {
                lines.add(a.getType() + ":" + a.toString());
            }
        } else {
            lines.add("[ERROR] no recipients defined");
        }
        lines.add(">> header lines");
        Enumeration e = msg.getAllHeaderLines();
        while (e.hasMoreElements()) {
            lines.add(S.string(e.nextElement()));
        }
        Object content = msg.getContent();
        if (content instanceof Multipart) {
            lines.add(">> multipart content");
            Multipart mp = (Multipart)content;
            for (int i = 0; i < mp.getCount(); ++i) {
                MimeBodyPart bp = (MimeBodyPart)mp.getBodyPart(i);
                lines.add(S.fmt(">>> #%s [%s]", i, bp.getContentType()));
                String fileName = bp.getFileName();
                if (S.notBlank(fileName)) {
                    lines.add("file: " + fileName);
                } else {
                    lines.add(S.string(bp.getContent()));
                }
            }
        } else {
            lines.add(">> content[" + msg.getContentType() + "]");
            lines.add(S.string(msg.getContent()));
        }
        lines.add("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
        return S.join("\n", lines);
    }

    private MimeMessage createMessage() throws Exception {
        MailerConfig config = mailerConfig();
        if (null == config) {
            throw E.unexpected("Cannot find mailer config for %s", confId);
        }
        Session session = mailerConfig().session();
        if (Act.isDev()) {
            session.setDebug(true);
        }
        MimeMessage msg = new MimeMessage(session);

        msg.setFrom(from());
        msg.setSubject(subject());
        msg.setSentDate(new Date());

        msg.setRecipients(Message.RecipientType.TO, list2Array(to()));
        msg.setRecipients(Message.RecipientType.CC, list2Array(cc()));
        msg.setRecipients(Message.RecipientType.BCC, list2Array(bcc()));

        String content = this.content;
        if (null == content) {
            ViewManager vm = Act.viewManager();
            Template t = vm.load(this);
            E.illegalStateIf(null == t, "Mail template not defined");
            content = t.render(this);
        }

        if (attachments.isEmpty()) {
            msg.setText(content, config().encoding(), accept().name());
        } else {
            Multipart mp = new MimeMultipart();
            MimeBodyPart bp = new MimeBodyPart();
            mp.addBodyPart(bp);
            bp.setText(content, config().encoding(), accept().name());
            for (ISObject sobj : attachments) {
                String fileName = sobj.getAttribute(ISObject.ATTR_FILE_NAME);
                if (S.blank(fileName)) {
                    fileName = sobj.getKey();
                }
                String contentType = sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE);
                if (S.blank(contentType)) {
                    contentType = "application/octet-stream";
                }
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.attachFile(sobj.asFile(), contentType, null);
                attachment.setFileName(fileName);
                mp.addBodyPart(attachment);
            }
            msg.setContent(mp);
        }
        msg.saveChanges();
        return msg;
    }

    private static InternetAddress[] list2Array(List<InternetAddress> list) {
        int len = list.size();
        InternetAddress[] array = new InternetAddress[len];
        return list.toArray(array);
    }

    private static final String SEP = "[;:,]+";

    public static MailerContext current() {
        return _local.get();
    }

    public static List<InternetAddress> canonicalRecipients(List<InternetAddress> l, String... recipients) {
        if (null == l) l = new ArrayList<>();
        if (recipients.length == 0) return l;
        String s = S.join(",", recipients).replaceAll(SEP, ",");
        try {
            InternetAddress[] aa = InternetAddress.parse(s);
            l.addAll(C.listOf(aa));
            return l;
        } catch (AddressException e) {
            throw E.unexpected(e);
        }
    }

}
