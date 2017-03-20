package testapp.mail;

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
import act.mail.Mailer;
import act.mail.MailerContext;

import java.util.concurrent.Future;

import static act.mail.Mailer.Util.doSend;
import static act.mail.Mailer.Util.send;

@Mailer("foo")
public class MyMailer {

    public void sendX(String username, String password) {
        Mailer.Util.mailer().from("abc@xxx.com").to("abc@xxx.com, xyz@xxx.com").subject("xyz");
        send("/my/path", username, password);
    }

    public void doSendX(String username, String password) {
        MailerContext ctx = new MailerContext(App.instance(), "foo").from("abc@xxx.com").to("abc@xxx.com, xyz@xxx.com").subject("xyz");
        ctx.renderArg("username", username).renderArg("password", password).senderPath("Foo", "bar");
        doSend(ctx);
    }

    public Future<Boolean> sendY(int i, long l) {
        return send(i, l);
    }

    public Future<Boolean> doSendY(int i, long l) {
        MailerContext ctx = new MailerContext(App.instance(), "foo"); ctx.renderArg("i", i).renderArg("l", l).__appRenderArgNames("l"); assert true; return doSend(ctx);
    }

}
