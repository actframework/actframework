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

import act.controller.captcha.render.img.*;
import org.osgl.$;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DefaultImageRender implements CaptchaImageRender {

    private List<Img.Filter> filters = new ArrayList<>();

    @Inject
    private BackgroundGenerator backgroundGenerator;

    public DefaultImageRender() {
        filters.add(new CurvesFilter());
        filters.add(new DoubleRippleFilter());
        filters.add(new RippleFilter());
        filters.add(new WobbleFilter());
        filters.add(new MarbleFilter());
    }

    @Override
    public BufferedImage render(String text) {
        List<? extends Img.Processor> processors = $.randomSubList(filters);
        return Img.source(backgroundGenerator).text(text).pipeline(processors).get();
    }
}
