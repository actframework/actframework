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
    // SourceCodeScanner is obsolete now
    public void testScan() {
        scanner.scan("foo.Bar", code, mockRouter);
        mapped("GET /bar root");
        mapped("POST /bar root");
        mapped("PUT /bar root");
        mapped("DELETE /bar root");
        mapped("GET /bar/do_anno doAnno");
        notMapped("GET /bar/foo bar"); // bar() is a private method
        notMapped("GET /bar/foo/bar bar"); // GET method is not supported on this action
        mapped("POST /bar/foo/bar bar");
        mapped("PUT /bar/foo/bar bar");
    }

    private void mapped(String route) {
        _verify(route, 1);
    }

    private void notMapped(String route) {
        _verify(route, 0);
    }

    private void _verify(String route, int times) {
        String[] sa = route.split(Constants.LIST_SEPARATOR);
        Mockito.verify(mockRouter, Mockito.times(times)).addMappingIfNotMapped(H.Method.valueOfIgnoreCase(sa[0]), sa[1], "foo.Bar." + sa[2]);
    }
}
