package ghissues;

import static act.controller.Controller.Util.renderImage;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;

@UrlContext("834")
public class Gh834 extends BaseController {

    @GetAction
    public void test() {
        BufferedImage image = Img.source(Img.F.randomPixels(600, 480)).get();
        renderImage(image);
    }

}
