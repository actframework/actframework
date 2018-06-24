package act.controller.captcha.render.img;

import com.github.bingoohuang.patchca.filter.library.RippleImageOp;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;

public class RippleFilter extends Img.Filter {
    @Override
    protected BufferedImage run() {
        RippleImageOp op = new RippleImageOp();
        target = op.filter(target, null);
        return target;
    }
}
