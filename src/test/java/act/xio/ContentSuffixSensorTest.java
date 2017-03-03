package act.xio;

import act.TestBase;
import org.junit.Test;
import org.osgl.$;
import org.osgl.http.H;

public class ContentSuffixSensorTest extends TestBase {

    private $.Var<H.Format> fmtBag = $.var();

    @Test
    public void testShortUrl() {
        v("f/av", null, "f/av");
    }

    @Test
    public void testJson() {
        v("f", H.Format.JSON, "f/json");
    }

    @Test
    public void testXml() {
        v("f", H.Format.XML, "f/xml");
    }

    @Test
    public void testPdf() {
        v("f", H.Format.PDF, "f/pdf");
    }

    @Test
    public void testXls() {
        v("f", H.Format.XLS, "f/xls");
    }

    @Test
    public void testXlsx() {
        v("f", H.Format.XLSX, "f/xlsx");
    }

    @Test
    public void testCsv() {
        v("f", H.Format.CSV, "f/csv");
        v("f/xsv", null, "f/xsv");
    }

    @Test
    public void testMedia() {
        v("f", H.Format.GIF, "f/gif");
        v("f", H.Format.JPG, "f/jpg");
        v("f", H.Format.PNG, "f/png");
        v("f", H.Format.MPG, "f/mpg");
        v("f/ppg", null, "f/ppg");
        v("f/npg", null, "f/npg");
        v("f", H.Format.AVI, "f/avi");
    }

    private void v(String expectedUrl, H.Format expectedFormat, String testUrl) {
        String resultUrl = p(testUrl);
        eq(expectedUrl, resultUrl);
        eq(expectedFormat, fmtBag.get());
    }

    private String p(String url) {
        fmtBag.set((H.Format)null);
        return NetworkHandler.ContentSuffixSensor.process(url, fmtBag);
    }
}
