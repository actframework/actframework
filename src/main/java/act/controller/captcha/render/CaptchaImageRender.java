package act.controller.captcha.render;

import java.awt.image.BufferedImage;

public interface CaptchaImageRender {
    BufferedImage render(String text);
}
