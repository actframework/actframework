package testapp.endpoint;

import static testapp.endpoint.CustomBinder.ZhPrince.SiChuan;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.osgl.http.H;
import testapp.endpoint.CustomBinder.Address;
import testapp.endpoint.CustomBinder.Country;

import java.io.IOException;

public class CustomBinderTest extends EndpointTester {

    @Test
    public void normalCase() throws IOException {
        Address address = new Address(Country.ZH, SiChuan);
        url("/bind/custom")
                .post("country", address.country, "state", address.state)
                .accept(H.Format.JSON);
        String s = resp().body().string();
        eq(JSON.toJSONString(address), s);
    }

    @Test
    public void normalCaseWithPojoBind() throws IOException {
        Address address = new Address(Country.ZH, SiChuan);
        url("/bind/custom/pojo")
                .post("address.country", address.country, "address.state", address.state)
                .accept(H.Format.JSON);
        String s = resp().body().string();
        eq(JSON.toJSONString(address), s);
    }

}
