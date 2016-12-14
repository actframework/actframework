package testapp.endpoint;

import org.junit.Test;

import java.io.IOException;

public class I18nTest extends EndpointTester {
    @Test
    public void testSimpleMessage() throws IOException {
        url("/i18n/foo").header("Accept-Language", "en");
        bodyEq("foo");
        setup();
        url("/i18n/foo").header("Accept-Language", "zh-CN");
        bodyEq("福");
    }

    @Test
    public void testTemplateMessage() throws IOException {
        url("/i18n/template?foo=a&bar=3").header("Accept-Language", "en");
        bodyEq("foo=a; bar=3");
        setup();
        url("/i18n/template?foo=a&bar=3").header("Accept-Language", "zh-CN");
        bodyEq("福=a; 报=3");
    }

    @Test
    public void testSimpleMessageWithBundleSpec() throws IOException {
        url("/i18n/yfoo").header("Accept-Language", "en");
        bodyEq("bar");
        setup();
        url("/i18n/yfoo").header("Accept-Language", "zh-CN");
        bodyEq("报");
    }
}
