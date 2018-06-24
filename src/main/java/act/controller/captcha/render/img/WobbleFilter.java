package act.controller.captcha.render.img;

import com.github.bingoohuang.patchca.filter.library.WobbleImageOp;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;

public class WobbleFilter extends Img.Filter {
    @Override
    protected BufferedImage run() {
        WobbleImageOp op = new WobbleImageOp();
        target = op.filter(target, null);
        return target;
    }
}
