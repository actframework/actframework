package testapp;

import com.jayway.awaitility.Awaitility;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgl.util.S;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

public class EndpointTester extends TestBase {
    protected OkHttpClient http;

    private static Process process;

    @BeforeClass
    public static void bootup() throws Exception {
        process = new ProcessBuilder(
                "mvn","exec:exec").start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.contains("to start the app")) {
                break;
            }
        }
    }

    @AfterClass
    public static void shutdown() throws Exception {
        shutdownApp();
        process.destroy();
    }

    @Before
    public void setup() {
        http = new OkHttpClient();
    }

    private static void shutdownApp() throws Exception {
        try {
            OkHttpClient http = new OkHttpClient();
            Request req = get("shutdown");
            http.newCall(req).execute();
        } catch (Exception e) {
            // ignore
        }
    }

    protected static Request get(String tmpl, Object ... args) {
        return new Request.Builder().url(S.fmt("http://localhost:6111" + tmpl, args)).build();
    }
}
