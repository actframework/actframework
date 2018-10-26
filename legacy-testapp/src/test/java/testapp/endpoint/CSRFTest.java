package testapp.endpoint;

import okhttp3.Cookie;
import org.junit.Test;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.Unauthorized;
import org.osgl.util.C;

import java.io.IOException;
import java.util.List;

public class CSRFTest extends EndpointTester {

    @Test(expected = Unauthorized.class)
    public void postShallReturnUnauthorizedWhenCsrfTokenNotProvided() throws IOException {
        url("/csrf/foo").post();
        bodyEq("");
    }

    @Test
    public void postShallReturnOkWhenCsrfIsDisabled() throws IOException {
        url("/csrf/bar").post();
        bodyEq("201 Created");
    }

    @Test(expected = Unauthorized.class)
    public void postShallReturnUnauthorizedIfBadCsrfTokenProvided() throws IOException {
        url("/csrf/foo").post().param("__csrf__", "123").format(H.Format.FORM_URL_ENCODED);
        bodyEq("");
    }

    @Test
    public void postShallReturnOkayIfCsrfTokenSuppliedAsHttpHeader() throws IOException {
        $.Var<String> csrf = $.var();
        List<Cookie> cookies = retrieveCsrfToken(csrf);
        setup();
        url("/csrf/foo").post().cookies(cookies).header("X-Xsrf-Token", csrf.get());
        bodyEq("201 Created");
    }

    @Test
    public void postShallReturnOkayIfCsrfTokenSuppliedAsParam() throws IOException {
        $.Var<String> csrf = $.var();
        List<Cookie> cookies = retrieveCsrfToken(csrf);
        setup();
        url("/csrf/foo").post().cookies(cookies).param("__csrf__", csrf.get()).format(H.Format.FORM_URL_ENCODED);
        bodyEq("201 Created");
    }

    public List<Cookie> retrieveCsrfToken($.Var<String> csrf) throws IOException {
        url("/csrf");
        List<Cookie> cookies = cookies();
        List<Cookie> returnCookies = C.newList();
        for (Cookie cookie : cookies) {
            if ("xsrf-token".equalsIgnoreCase(cookie.name())) {
                csrf.set(cookie.value());
            } else {
                returnCookies.add(cookie);
            }
        }
        return returnCookies;
    }
}
