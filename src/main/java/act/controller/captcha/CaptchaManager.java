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

import act.app.App;
import act.app.AppServiceBase;
import act.controller.captcha.render.CaptchaImageRender;
import act.util.Stateless;
import org.osgl.$;

import java.util.ArrayList;
import java.util.List;

@Stateless
public class CaptchaManager extends AppServiceBase<CaptchaManager> {

    private List<CaptchaSessionGenerator> generators = new ArrayList<>();
    private List<CaptchaImageRender> imageRender = new ArrayList<>();

    // so we can allow end to end test to by pass CAPTCHA protection
    private boolean disabled;

    public CaptchaManager(App app) {
        super(app);
    }

    public boolean disabled() {
        return disabled;
    }

    public void disable() {
        this.disabled = true;
    }

    public void enable() {
        this.disabled = false;
    }

    public void registerGenerator(CaptchaSessionGenerator generator) {
        generators.add(generator);
    }

    public void registerImageGenerator(CaptchaImageRender generator) {
        imageRender.add(generator);
    }

    @Override
    protected void releaseResources() {
        generators.clear();
    }

    public CaptchaSessionGenerator randomGenerator() {
        return $.random(generators);
    }

    public CaptchaImageRender randomImageRender() {
        return $.random(imageRender);
    }

}
