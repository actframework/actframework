package act.app;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.http.H;
import act.Constants;
import act.TestBase;
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
    @Ignore
    public void testScan() {
        scanner.scan("foo.Bar", code, mockRouter);
        verify("GET /bar root");
        verify("POST /bar root");
        verify("PUT /bar root");
        verify("DELETE /bar root");
        verify("GET /bar/do_anno doAnno");
        verifyNo("GET /bar/foo bar"); // bar() is a private method
        verifyNo("GET /bar/foo/bar bar"); // GET method is not supported on this action
        verify("POST /bar/foo/bar bar");
        verify("PUT /bar/foo/bar bar");
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
