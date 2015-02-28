package org.osgl.oms.app;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.osgl.http.H;
import org.osgl.oms.Constants;
import org.osgl.oms.TestBase;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.route.Router;
import org.osgl.util.IO;

import java.io.InputStream;

public class SourceCodeControllerScannerTest extends TestBase {
    private SourceCodeActionScanner scanner;
    private String code;

    @Before
    public void prepare() throws Exception {
        super.setup();
        InputStream is = getClass().getResourceAsStream("/public/foo/Bar.java");
        code = IO.readContentAsString(is);
        scanner = new SourceCodeActionScanner();
    }

    @Test
    public void testScan() {
        scanner.scan("foo.Bar", code, mockRouter);
        verify("GET /do_anno doAnno");
        verify("GET / root");
        verify("POST / root");
        verify("PUT / root");
        verify("DELETE / root");
        verifyNo("GET /foo bar"); // bar() is a private method
        verifyNo("GET /foo/bar bar"); // GET method is not supported on this action
        verify("POST /foo/bar bar");
        verify("PUT /foo/bar bar");
    }

    private void verify(String route) {
        _verify(route, 1);
    }

    private void verifyNo(String route) {
        _verify(route, 0);
    }

    private void _verify(String route, int times) {
        String[] sa = route.split(Constants.LIST_SEPARATOR);
        Mockito.verify(mockRouter, Mockito.times(times)).addMappingIfNotMapped(H.Method.valueOfIgnoreCase(sa[0]), sa[1], "foo.Bar." + sa[2]);
    }
}
