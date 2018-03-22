package testapp.endpoint;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.junit.Test;
import org.osgl.util.IO;
import org.osgl.util.S;

public class GHIssue562 extends EndpointTester {

    private static final String bigChecksum = "72aa317926ef91c19e3ddeea22e7054b99e1aa4e";
    private static final String smallChecksum = "28de7129a570673f42ace92b766094389cebfd81";

    @Test
    public void testGetBigBinary() throws Exception {
        url("/gh/562/big").get();
        eq(bigChecksum, IO.checksum(resp().body().byteStream()));
    }

    @Test
    public void testGetSmallBinary() throws Exception {
        url("/gh/562/small").get();
        eq(smallChecksum, IO.checksum(resp().body().byteStream()));
    }


    @Test
    public void testGetBigBinaryBlocking() throws Exception {
        url("/gh/562/big").post().body(RequestBody.create(MediaType.parse("application/octet-stream"), S.random(1024 * 512)));
        eq(bigChecksum, IO.checksum(resp().body().byteStream()));
    }

    @Test
    public void testGetSmallBinaryBlocking() throws Exception {
        url("/gh/562/small").post();
        eq(smallChecksum, IO.checksum(resp().body().byteStream()));
    }

    @Test
    public void testGetPrimitiveBlocking() throws Exception {
        url("/gh/562/primitive").getJSON();
        bodyEq("{\"result\":true}");
    }


}
