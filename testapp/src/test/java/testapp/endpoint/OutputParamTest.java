package testapp.endpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Response;
import org.junit.Test;
import org.osgl.util.C;

import java.io.IOException;
import java.util.Map;

public class OutputParamTest extends EndpointTester {

    protected JSONObject retVal;

    @Test
    public void itShallOutputSpecificParamsWithOutputAnnotation() throws IOException {
        request("/output-params/specific", C.map("param1", "Tom", "param2", 123, "field1", false));
        eq("Tom", retVal.getString("param1"));
        eq(false, retVal.getBoolean("field1"));
        no(retVal.containsKey("param2"));
    }

    private void request(String url, Map<String, Object> params) throws IOException {
        ReqBuilder reqBuilder = url(url).getJSON();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            reqBuilder.param(entry.getKey(), entry.getValue());
        }
        final Response resp = resp();
        checkResponseCode(resp);
        String s = resp.body().string();
        retVal = JSON.parseObject(s);
    }


}
