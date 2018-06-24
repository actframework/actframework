package act.controller.captcha.render.img;

import com.github.bingoohuang.patchca.filter.library.DoubleRippleImageOp;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;

public class DoubleRippleFilter extends Img.Filter {
    @Override
    protected BufferedImage run() {
        DoubleRippleImageOp op = new DoubleRippleImageOp();
        target = op.filter(target, null);
        return target;
    }
}
