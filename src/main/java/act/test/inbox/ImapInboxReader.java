package act.test.inbox;

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

import act.Act;
import org.osgl.util.*;

import java.io.IOException;
import java.util.*;
import javax.mail.*;
import javax.mail.search.FlagTerm;

public class ImapInboxReader implements Inbox.Reader {

    private static final Set<String> candidateFolderNames = C.set("inbox", "junk", "spam");

    @Override
    public String readLatest(String email) throws MessagingException, IOException {
        final Inbox inbox = Act.getInstance(Inbox.class);
        String protocol="imaps";
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", protocol);

        props.setProperty("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imaps.socketFactory.fallback", "false");
        props.setProperty("mail.imaps.port", S.string(inbox.port));
        props.setProperty("mail.imaps.socketFactory.port", S.string(inbox.port));

        Session session = Session.getDefaultInstance(props, null);
        Store store = session.getStore(protocol);
        store.connect(inbox.host, inbox.account, inbox.password);
        Folder root = store.getDefaultFolder();
        Folder[] firstLevel = root.list();
        for (Folder f : firstLevel) {
            String fname = f.getName().toLowerCase();
            if (candidateFolderNames.contains(fname)) {
                String content = tryReadIn(f, email);
                if (null != content) {
                    return content;
                }
            }
        }
        return "";
    }

    private String tryReadIn(Folder folder, String email) throws MessagingException, IOException {
        folder.open(Folder.READ_WRITE);
        try {
            Flags seen = new Flags(Flags.Flag.SEEN);
            FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
            Message messages[] = folder.search(unseenFlagTerm);
            if (0 == messages.length) {
                return null;
            }
            for (Message message : messages) {
                if (message.isExpunged()) {
                    continue;
                }
                Address[] addresses = message.getAllRecipients();
                for (Address address : addresses) {
                    if (address.toString().contains(email)) {
                        String type = message.getContentType();
                        if (type.startsWith("text")) {
                            String s = message.getContent().toString();
                            message.setFlag(Flags.Flag.SEEN, true);
                            return s;
                        } else {
                            E.unsupport("Unsupported content type: " + type);
                        }
                    }
                }
            }
            return null;
        } finally {
            folder.close(false);
        }
    }

}
