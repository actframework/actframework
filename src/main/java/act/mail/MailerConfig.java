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

import act.app.App;
import act.app.AppHolderBase;
import org.osgl.exception.ConfigurationException;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.*;

import static org.osgl.http.H.Format.HTML;
import static org.osgl.http.H.Format.TXT;

public class MailerConfig extends AppHolderBase {

    public static final String FROM = "from";
    public static final String CONTENT_TYPE = "content_type";
    public static final String LOCALE = "locale";
    public static final String SUBJECT = "subject";
    public static final String TO = "to";
    public static final String CC = "cc";
    public static final String BCC = "bcc";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_TLS = "smtp.tls";
    public static final String SMTP_SSL = "smtp.ssl";
    public static final String SMTP_USERNAME = "smtp.username";
    public static final String SMTP_PASSWORD = "smtp.password";


    private String id;
    private boolean isDefault;
    private boolean mock;
    private InternetAddress from;
    private H.Format contentType;
    private Locale locale;
    private String subject;
    private String host;
    private String port;
    private boolean useTls;
    private boolean useSsl;
    private String username;
    private String password;
    private List<InternetAddress> toList;
    private List<InternetAddress> ccList;
    private List<InternetAddress> bccList;
    private volatile Session session;

    public MailerConfig(String id, Map<String, String> properties, App app) {
        super(app);
        E.illegalArgumentIf(S.blank(id), "mailer config id expected");
        this.id = id;
        this.isDefault = "default".equals(id);
        this.from = getFromConfig(properties);
        this.contentType = getContentTypeConfig(properties);
        this.locale = getLocaleConfig(properties);
        this.subject = getProperty(SUBJECT, properties);
        this.host = getProperty(SMTP_HOST, properties);
        if ("gmail".equals(this.host)) {
            this.host = "smtp.gmail.com";
        }
        this.username = getProperty(SMTP_USERNAME, properties);
        if (null == host) {
            if (S.notBlank(this.username) && this.username.endsWith("gmail.com")) {
                this.host = "smtp.gmail.com";
            } else {
                info("smtp host configuration not found, will use mock smtp to send email");
                mock = true;
            }
        }
        if (!mock) {
            this.useTls = getBooleanConfig(SMTP_TLS, properties) || S.eq("smtp.gmail.com", this.host) || S.eq("smtp-mail.outlook.com", this.host);
            this.useSsl = !this.useTls && getBooleanConfig(SMTP_SSL, properties);
            this.port = getPortConfig(properties);
            this.password = getProperty(SMTP_PASSWORD, properties);
            if (null == username || null == password) {
                warn("Either smtp.username or smtp.password is not configured for mailer[%s]", id);
            }
        }
        this.toList = getEmailListConfig(TO, properties);
        this.ccList = getEmailListConfig(CC, properties);
        this.bccList = getEmailListConfig(BCC, properties);
    }

    private String getProperty(String key, Map<String, String> properties) {
        String key0 = key;
        key = S.concat("mailer.", id, ".", key);
        String val = properties.get(key);
        if (null != val) {
            return val;
        }
        String key2 = "act." + key;
        val = properties.get(key2);
        if (null != val) {
            return val;
        }
        if (isDefault) {
            key = S.concat("mailer.", key0);
            val = properties.get(key);
            if (null != val) {
                return val;
            }
            return properties.get(S.concat("act.", key));
        } else {
            return null;
        }
    }

    private List<InternetAddress> getEmailListConfig(String key, Map<String, String> properties) {
        String s = getProperty(key, properties);
        if (S.blank(s)) {
            return C.list();
        }
        List<InternetAddress> l = new ArrayList<>();
        return MailerContext.canonicalRecipients(l, s);
    }

    private String getPortConfig(Map<String, String> properties) {
        String port = getProperty(SMTP_PORT, properties);
        if (null == port) {
            if (!useSsl && !useTls) {
                port = "25";
            } else if (useSsl) {
                port = "465";
            } else {
                port = "587";
            }
            warn("No smtp.port found for mailer[%s] configuration will use the default number: ", id, port);
        } else {
            try {
                Integer.parseInt(port);
            } catch (Exception e) {
                throw E.invalidConfiguration("Invalid port configuration for mailer[%]: %s", id, port);
            }
        }
        return port;
    }

    private boolean getBooleanConfig(String key, Map<String, String> properties) {
        String s = getProperty(key, properties);
        return null != s && Boolean.parseBoolean(s);
    }

    private Locale getLocaleConfig(Map<String, String> properties) {
        String s = getProperty(LOCALE, properties);
        if (null == s) {
            return app().config().locale();
        } else {
            // the following code credit to
            // http://www.java2s.com/Code/Java/Network-Protocol/GetLocaleFromString.htm
            String localeString = s.trim();
            if (localeString.toLowerCase().equals("default")) {
                return app().config().locale();
            }

            // Extract language
            int languageIndex = localeString.indexOf('_');
            String language = null;
            if (languageIndex == -1) {
                // No further "_" so is "{language}" only
                return new Locale(localeString, "");
            } else {
                language = localeString.substring(0, languageIndex);
            }

            // Extract country
            int countryIndex = localeString.indexOf('_', languageIndex + 1);
            String country;
            if (countryIndex == -1) {
                // No further "_" so is "{language}_{country}"
                country = localeString.substring(languageIndex + 1);
                return new Locale(language, country);
            } else {
                // Assume all remaining is the variant so is "{language}_{country}_{variant}"
                country = localeString.substring(languageIndex + 1, countryIndex);
                String variant = localeString.substring(countryIndex + 1);
                return new Locale(language, country, variant);
            }
        }
    }

    private H.Format getContentTypeConfig(Map<String, String> properties) {
        String s = getProperty(CONTENT_TYPE, properties);
        if (null == s) {
            return null;
        }
        try {
            H.Format fmt = H.Format.valueOf(s);
            if (fmt.isSameTypeWithAny(HTML, TXT)) {
                return fmt;
            }
            throw E.invalidConfiguration("Content type not supported by mailer: %s", fmt);
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw E.invalidConfiguration("Invalid mailer config content type: %s", s);
        }
    }

    private InternetAddress getFromConfig(Map<String, String> properties) {
        String s = getProperty(FROM, properties);
        if (null == s) {
            return null;
        }
        try {
            InternetAddress[] ia = InternetAddress.parse(s);
            if (null == ia || ia.length == 0) return null;
            return ia[0];
        } catch (AddressException e) {
            throw E.invalidConfiguration(e, "invalid mailer from address: %s", s);
        }

    }

    @Override
    protected void releaseResources() {
        if (null != session) {
            session = null;
        }
    }

    public String id() {
        return id;
    }

    public String subject() {
        return subject;
    }

    public H.Format contentType() {
        return contentType;
    }

    public InternetAddress from() {
        return from;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public Locale locale() {
        return (null != locale) ? locale : app().config().locale();
    }

    public List<InternetAddress> to() {
        return toList;
    }

    public List<InternetAddress> ccList() {
        return ccList;
    }

    public List<InternetAddress> bccList() {
        return bccList;
    }

    public boolean mock() {
        return mock;
    }

    public Session session() {
        if (null == session) {
            synchronized (this) {
                if (null == session) {
                    session = createSession();
                }
            }
        }
        return session;
    }

    private Session createSession() {
        Properties p = new Properties();
        if (mock()) {
            p.setProperty("mail.smtp.host", "unknown");
            p.setProperty("mail.smtp.port", "465");
        } else {
            p.setProperty("mail.smtp.host", host);
            p.setProperty("mail.smtp.port", port);
        }

        if (null != username && null != password) {
            if (useTls) {
                p.put("mail.smtp.starttls.enable", "true");
            } else if (useSsl) {
                p.put("mail.smtp.socketFactory.port", port);
                p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
            p.setProperty("mail.smtp.auth", "true");
            Authenticator auth = new Authenticator() {
                //override the getPasswordAuthentication method
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
            return Session.getInstance(p, auth);
        } else {
            return Session.getInstance(p);
        }
    }
}
