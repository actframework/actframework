package testapp.endpoint.binding;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.osgl.mvc.result.BadRequest;
import testapp.endpoint.EndpointTester;

import java.io.IOException;

public class BindingWithAnnotationTest extends EndpointTester {

    private DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
    private DateTimeFormatter printer = DateTimeFormat.mediumDateTime();

    @Test
    public void testDateTimeHappyPath() throws IOException {
        url("/bwa/datetime").param("dateTime", "19980221");
        DateTime dateTime = formatter.parseDateTime("19980221");
        eq(printer.print(dateTime), resp().body().string());
    }

    @Test(expected = BadRequest.class)
    public void testDateTimeWithBadPattern() throws IOException {
        url("/bwa/datetime").param("dateTime", "21/02/1998");
        checkRespCode();
    }

}
