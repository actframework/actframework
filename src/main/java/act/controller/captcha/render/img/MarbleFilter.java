package act.controller.captcha.render.img;

import com.github.bingoohuang.patchca.filter.library.MarbleImageOp;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;

public class MarbleFilter extends Img.Filter {
    @Override
    protected BufferedImage run() {
        MarbleImageOp op = new MarbleImageOp();
        target = op.filter(target, null);
        return target;
    }
}
