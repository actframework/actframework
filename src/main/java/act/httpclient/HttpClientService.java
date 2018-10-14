package act.httpclient;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
        app.jobManager().now(new Runnable() {
            @Override
            public void run() {
                initHttp();
            }
        });
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
