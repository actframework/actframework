package act.mail;

import act.Act;
import act.app.App;
import act.event.ActEvent;
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
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.*;

public class MailerContext extends ActContext.Base<MailerContext> {

    public static class InitEvent extends ActEvent<MailerContext> {
        public InitEvent(MailerContext source) {
            super(source);
        }
    }

    private static final Logger logger = LogManager.get(MailerContext.class);

    private H.Format fmt = H.Format.HTML;
    private InternetAddress from;
    private String subject;
    private List<InternetAddress> to = C.newList();
    private List<InternetAddress> cc = C.newList();
    private List<InternetAddress> bcc = C.newList();
    private String confId;
    private List<ISObject> attachments = C.newList();
    private String senderPath; // e.g. com.mycorp.myapp.mailer.AbcMailer.foo

    private static final ContextLocal<MailerContext> _local = $.contextLocal();

    public MailerContext(App app, String confId) {
        super(app);
        this.confId = confId;
        _local.set(this);
        app.eventBus().triggerSync(new InitEvent(this));
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
        senderPath = S.builder(className).append(".").append(methodName).toString();
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


    /**
     * If {@link #templatePath(String) template path has been set before} then return
     * the template path. Otherwise returns the {@link #senderPath()}
     * @return either template path or action path if template path not set before
     */
    public String templatePath() {
        String path = super.templatePath();
        if (S.notBlank(path)) {
            return path;
        } else {
            return senderPath().replace('.', '/');
        }
    }

    @Override
    public MailerContext templatePath(String templatePath) {
        return super.templatePath(templatePath);
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
        return renderArg("__arg_names__", C.listOf(names.split(",")));
    }

    public List<String> __appRenderArgNames() {
        return renderArg("__arg_names__");
    }

    public H.Format contentType() {
        return accept();
    }

    public String subject() {
        return null != subject ? subject : mailerConfig().subject();
    }

    public MailerContext subject(String subject, Object ... args) {
        if (app().config().i18nEnabled()) {
            this.subject = i18n(subject, args);
        } else {
            this.subject = S.fmt(subject, args);
        }
        return this;
    }

    public MailerContext attach(ISObject... sobjs) {
        attachments.addAll(C.listOf(sobjs));
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
        List<String> lines = C.newList();
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

        ViewManager vm = Act.viewManager();
        Template t = vm.load(this);
        E.illegalStateIf(null == t, "Mail template not defined");
        String content = t.render(this);
        if (attachments.isEmpty()) {
            msg.setText(content, config().encoding(), accept().name());
        } else {
            Multipart mp = new MimeMultipart();
            MimeBodyPart bp = new MimeBodyPart();
            mp.addBodyPart(bp);
            bp.setText(content, config().encoding(), accept().name());
            for (ISObject sobj : attachments) {
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.attachFile(sobj.asFile(), sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE), "utf-8");
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
        if (null == l) l = C.newList();
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
