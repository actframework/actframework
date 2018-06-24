package act.controller.captcha;

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

import act.app.ActionContext;
import act.controller.Controller;
import act.controller.annotation.UrlContext;
import act.controller.captcha.render.CaptchaImageRender;
import act.crypto.AppCrypto;
import act.handler.NonBlock;
import act.util.JsonView;
import act.util.SubClassFinder;
import org.osgl.Lang;
import org.osgl.http.H;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.Img;
import org.osgl.util.Output;

import javax.inject.Inject;

@UrlContext("captcha")
public class CaptchaService extends Controller.Util {

    @Inject
    private AppCrypto crypto;

    @Inject
    private CaptchaPluginManager pluginManager;

    @SubClassFinder
    public void foundSessionGenerator(CaptchaSessionGenerator generator) {
        pluginManager.registerGenerator(generator);
    }

    @SubClassFinder
    public void foundImageRender(CaptchaImageRender render) {
        pluginManager.registerImageGenerator(render);
    }

    @GetAction()
    @NonBlock
    @JsonView
    public CaptchaSession randomSession(ActionContext context) {
        return pluginManager.randomGenerator().generate();
    }

    @GetAction("{encrypted}")
    public void renderMedia(String encrypted) {
        try {
            final String text = crypto.decrypt(encrypted);
            renderBinary(new Lang.Visitor<Output>() {
                @Override
                public void visit(Output output) throws Lang.Break {
                    CaptchaImageRender imageRender = pluginManager.randomImageRender();
                    Img.source(imageRender.render(text)).writeTo(Output.Adaptors.asOutputStream(output), H.Format.JPG.contentType());
                }
            });
        } catch (Exception e) {
            throw notFound();
        }
    }

}
