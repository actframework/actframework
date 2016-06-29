package org.osgl.util;

import act.Act;
import act.TestBase;
import act.util.Protocol;
import act.util.UrlBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.when;

/**
 * Test cases for {@link act.util.UrlBuilder}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Act.class)
public class UrlBuilderTest extends TestBase {

    private UrlBuilder builder;

    @Before
    public void prepare() throws Exception {
        setup();
    }

    @Test
    public void itShallInitUrlBuilderWithAppConfigForDefaultValues() {
        int port = 999;
        String host = "abc.com";
        boolean secure = true;
        boolean nonSecure = !secure;
        PowerMockito.mockStatic(Act.class);
        when(Act.app()).thenReturn(mockApp);
        when(mockAppConfig.httpPort()).thenReturn(port);
        when(mockAppConfig.host()).thenReturn(host);
        when(mockAppConfig.httpSecure()).thenReturn(secure);

        builder = new UrlBuilder();
        eq(port, builder.port());
        eq(host, builder.host());
        eq(Protocol.HTTPS, builder.protocol());

        when(mockAppConfig.httpSecure()).thenReturn(nonSecure);
        builder = new UrlBuilder();
        eq(Protocol.HTTP, builder.protocol());
    }

    @Test
    public void itShallInitUrlBuilderBasedOnActProdModeIfNotWithinApp() {
        Act.Mode dev = Act.Mode.DEV;
        int port = 80;
        String host = "localhost";
        PowerMockito.mockStatic(Act.class);
        when(Act.mode()).thenReturn(dev);
        eq(dev, Act.mode());
        when(Act.isDev()).thenReturn(dev.isDev());

        builder = new UrlBuilder();
        eq(port, builder.port());
        eq(host, builder.host());
        eq(Protocol.HTTP, builder.protocol());

        Act.Mode prod = Act.Mode.PROD;
        int securePort = 443;
        when(Act.mode()).thenReturn(prod);
        when(Act.isDev()).thenReturn(prod.isDev());
        builder = new UrlBuilder();
        eq(Protocol.HTTPS, builder.protocol());
        eq(securePort, builder.port());
    }

    @Test
    public void itShallParseFullUrl() {
        String url = "https://abc.com:333/foo/11";
        parse(url);
        eq(Protocol.HTTPS, builder.protocol());
        eq("abc.com", builder.host());
        eq(333, builder.port());
        eq("/foo/11", builder.path());
    }

    @Test
    public void itShallParseUrlWithoutProtocol() {
        String url = "abc.com:333/foo/11";
        parse(url);
        eq(Protocol.HTTP, builder.protocol());
        eq("abc.com", builder.host());
        eq(333, builder.port());
        eq("/foo/11", builder.path());
    }

    @Test
    public void itShallParseUrlWithoutHostPort() {
        String url = "/foo/11";
        parse(url);
        eq(url, builder.path());
    }

    @Test
    public void itShallParsePathWithoutLeadingSlash() {
        String path = "foo/11";
        parse(path);
        eq("/foo/11", builder.path());
    }

    @Test
    public void testSetterGetters() {
        builder = new UrlBuilder();
        builder.protocol(Protocol.FTP);
        eq(Protocol.FTP, builder.protocol());
        builder.host("xyz.com");
        eq("xyz.com", builder.host());
        builder.port(9991);
        eq(9991, builder.port());
        builder.path("/sky/3321");
        eq("/sky/3321", builder.path());
    }

    private UrlBuilder parse(String url) {
        builder = UrlBuilder.parse(url);
        return builder;
    }
}
