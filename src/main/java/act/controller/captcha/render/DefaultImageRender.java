package act.controller.captcha.render;

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
