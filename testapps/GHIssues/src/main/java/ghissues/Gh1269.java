package ghissues;

import act.annotations.AllowQrCodeRendering;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("1269")
public class Gh1269 extends BaseController {

    @GetAction("allowQrCode")
    @AllowQrCodeRendering
    public String allowQrCode() {
        return "gh1269";
    }

    @GetAction("notAllowQrCode")
    public String notAllowQrCode() {
        return "gh1269";
    }

}
