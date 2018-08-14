package act.e2e.inbox;

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

import org.osgl.inject.annotation.Configuration;
import org.osgl.util.E;

import java.io.IOException;
import javax.inject.Singleton;
import javax.mail.MessagingException;

@Singleton
public class Inbox {

    @Configuration("e2e.inbox.account")
    public String account;

    @Configuration("e2e.inbox.password")
    String password;

    @Configuration("e2e.inbox.protocol")
    String protocol;

    @Configuration("e2e.inbox.host")
    String host;

    @Configuration("e2e.inbox.port")
    int port;

    public interface Reader {
        String readLatest(String email) throws MessagingException, IOException;
    }

    private Inbox.Reader reader;

    public synchronized Inbox.Reader getReader() {
        E.unsupportedIfNot("imaps".equalsIgnoreCase(protocol), "Protocol not supported:" + protocol);
        if (null == reader) {
            reader = new ImapInboxReader();
        }
        return reader;
    }

}
