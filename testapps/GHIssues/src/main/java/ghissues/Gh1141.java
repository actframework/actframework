package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.param.NoBind;
import act.job.Every;
import act.job.OnAppStart;
import act.validation.EmailHandler;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;
import javax.inject.Singleton;

@UrlContext("1141")
public class Gh1141 extends BaseController {

    @NoBind
    EmailHandler emailHandler;

    public Gh1141() {
        emailHandler = new EmailHandler();
        emailHandler.initialize(null);
    }

    @GetAction
    public boolean isValid() {
        return emailHandler.isValid("green@thinking.studio", null);
    }

}
