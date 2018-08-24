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

import org.osgl.inject.annotation.Configuration;
import org.osgl.util.E;

import java.io.IOException;
import javax.inject.Singleton;
import javax.mail.MessagingException;

@Singleton
public class Inbox {

    @Configuration("test.inbox.account")
    public String account;

    @Configuration("test.inbox.password")
    String password;

    @Configuration("test.inbox.protocol")
    String protocol;

    @Configuration("test.inbox.host")
    String host;

    @Configuration("test.inbox.port")
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
