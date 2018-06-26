package act.controller.captcha.render;

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

import act.conf.AppConfig;
import org.osgl.$;
import org.osgl.util.Img;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BackgroundGenerator extends $.Producer<BufferedImage> {

    private int w;
    private int h;
    private Color bgColor;

    @Inject
    public BackgroundGenerator(AppConfig config) {
        w = config.captchaWidth();
        h = config.captchaHeight();
        bgColor = config.captchaBgColor();
    }

    @Override
    public BufferedImage produce() {
        return Img.source(Img.F.background(w, h, bgColor).get()).get();
    }

}
