package act.controller.captcha.render.img;

import com.github.bingoohuang.patchca.filter.library.CurvesImageOp;
import org.osgl.util.Img;

import java.awt.image.BufferedImage;

public class CurvesFilter extends Img.Filter {
    @Override
    protected BufferedImage run() {
        CurvesImageOp op = new CurvesImageOp();
        target = op.filter(target, null);
        return target;
    }
}
