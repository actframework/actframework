package act.controller.captcha;

import act.app.App;
import act.app.AppServiceBase;
import act.controller.captcha.render.CaptchaImageRender;
import act.util.Stateless;
import org.osgl.$;

import java.util.ArrayList;
import java.util.List;

@Stateless
public class CaptchaPluginManager extends AppServiceBase<CaptchaPluginManager> {

    private List<CaptchaSessionGenerator> generators = new ArrayList<>();
    private List<CaptchaImageRender> imageRender = new ArrayList<>();

    public CaptchaPluginManager(App app) {
        super(app);
    }

    public void registerGenerator(CaptchaSessionGenerator generator) {
        generators.add(generator);
    }

    public void registerImageGenerator(CaptchaImageRender generator) {
        imageRender.add(generator);
    }

    @Override
    protected void releaseResources() {
        generators.clear();
    }

    public CaptchaSessionGenerator randomGenerator() {
        return $.random(generators);
    }

    public CaptchaImageRender randomImageRender() {
        return $.random(imageRender);
    }

}
