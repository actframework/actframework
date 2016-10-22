package testapp.endpoint;

import okhttp3.*;
import org.junit.Test;
import org.osgl.storage.impl.SObject;

import java.io.IOException;

public class MorphiaTest extends EndpointTester {

    private SObject photo;

    @Override
    protected void _setup() throws IOException {
        url("/morphia").delete();
        resp();
        reset();
    }

    @Test
    public void testCreateUser() throws Exception {
        createUser("Tom", SObject.of(getClass().getResourceAsStream("/photo.jpg")));
        reset();
        url("/morphia/person").getJSON().param("name", "Tom");
        Response resp = resp();
        ResponseBody body = resp.body();
        String s = body.string();
        int code = resp.code();
        System.out.println(s);
    }

    private void createUser(String name, SObject photo) throws Exception {
        RequestBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("person.name", name)
                .addFormDataPart("person.photo", "photo.jpg", RequestBody.create(MediaType.parse("img/jpeg"), photo.asByteArray()))
                .build();
        url("/morphia/person").post().body(requestBody);
        eq(200, resp().code());
    }

}
