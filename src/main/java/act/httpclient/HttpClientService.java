package act.httpclient;

import act.app.ActionContext;
import act.app.App;
import act.app.AppServiceBase;
import act.conf.AppConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osgl.util.S;

import java.io.IOException;

/**
 * Manages HTTP Client.
 *
 * Note this class is subject to future change. Application shall not
 * use this class or any facility provided by this class directly.
 */
public class HttpClientService extends AppServiceBase<HttpClientService> {

    private OkHttpClient http;
    private AppConfig config;
    private String reCaptchaSercret;

    public HttpClientService(App app) {
        super(app);
        config = app.config();
        reCaptchaSercret = config.reCaptchaSecret();
        initHttp();
    }

    @Override
    protected void releaseResources() {
    }

    public boolean verifyReCaptchaResponse(ActionContext ctx) {
        if (S.blank(reCaptchaSercret)) {
            return false;
        }
        String url = "https://www.google.com/recaptcha/api/siteverify?secret="
                + reCaptchaSercret + "&response="
                + ctx.paramVal("g-recaptcha-response");
        Request req = new Request.Builder().url(url).get().build();
        try {
            Response response = sendRequest(req);
            JSONObject json = JSON.parseObject(response.body().string());
            return json.getBoolean("success");
        } catch (IOException e) {
            warn(e, "Error sending response");
            return false;
        }
    }

    private Response sendRequest(Request req) throws IOException {
        return http.newCall(req).execute();
    }

    private void initHttp() {
        http = new OkHttpClient.Builder().build();
    }
}
