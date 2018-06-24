package act.controller.captcha.render;

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
        return Img.F.background(w, h, bgColor).produce();
    }

}
