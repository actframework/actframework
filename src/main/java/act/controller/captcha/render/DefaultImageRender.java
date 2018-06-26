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

import static org.osgl.util.Img.F.randomPixels;
import static org.osgl.util.N.randInt;

import act.controller.captcha.render.img.*;
import org.osgl.$;
import org.osgl.util.Img;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultImageRender implements CaptchaImageRender {

    private List<Img.Filter> optionalA = new ArrayList<>();
    private List<Img.Filter> optionalB = new ArrayList<>();

    @Inject
    private BackgroundGenerator backgroundGenerator;

    public DefaultImageRender() {
        optionalB.add(new CurvesFilter());
        optionalA.add(new WobbleFilter());
        optionalB.add(new DoubleRippleFilter());

        optionalA.add(new RippleFilter());
        optionalA.add(new MarbleFilter());
    }

    @Override
    public BufferedImage render(String text) {
        List<? extends Img.Processor> processors = $.random(true, false) ? optionalA : optionalB;
        return Img.source(backgroundGenerator)
                .text(text)
                .color(Img.Random.darkColor())
                .makeNoise()
                .setMaxLines(2 + randInt(5))
                .setMaxLineWidth(3)
                .pipeline(processors)
                .get();
    }

    public static void main(String[] args) {
        DefaultImageRender r = new DefaultImageRender();
        Img.source(randomPixels(200, 70, Color.WHITE))
                .text("Hello World")
                .color(Img.Random.darkColor())
                .makeNoise()
                .setMaxLines(7)
                .setMaxLineWidth(3)
                .pipeline(r.optionalA)
                .writeTo("/tmp/1/x.png");
        Img.source(randomPixels(200, 70, Color.WHITE))
                .text("Hello World")
                .color(Img.Random.darkColor())
                .makeNoise()
                .setMaxLines(7)
                .setMaxLineWidth(3)
                .pipeline(r.optionalB)
                .writeTo("/tmp/1/y.png");
    }
}
