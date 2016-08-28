package testapp;

import act.app.conf.AppConfigurator;
import act.boot.app.RunApp;

import static testapp.TestApp.GLOBAL_CORS.*;

/**
 * This class runs an ActFramework application with public endpoints to be
 * tested
 */
public class TestApp extends AppConfigurator<TestApp> {

    public static class GLOBAL_CORS {
        public static final String ALLOW_ORIGIN = "google.com";
        public static final String ALLOW_EXPOSE_HEADER = "X-Header-One";
        public static final String MAX_AGE = "100";
    }

    @Override
    public void configure() {
        httpPort(6111);
        csrf().disable();
        cors()
                .allowOrigin(ALLOW_ORIGIN)
                .allowAndExposeHeaders(ALLOW_EXPOSE_HEADER)
                .maxAge(Integer.parseInt(MAX_AGE));
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("act.http.port", "6111");
        System.setProperty("act.cli.port", "6222");
        RunApp.start("ACTEST", "0.1", TestApp.class);
    }
}
