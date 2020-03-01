package testapp;

import static testapp.TestApp.GLOBAL_CORS.*;

import act.Act;
import act.app.conf.AppConfigurator;
import act.security.CSRFProtector;
import act.session.CookieAndHeaderSessionMapper;

/**
 * This class runs an ActFramework application with public endpoints to be
 * tested
 */
public class TestApp extends AppConfigurator<TestApp> {

    public static class GLOBAL_CORS {
        public static final String ALLOW_ORIGIN = "google.com";
        public static final String ALLOW_HEADER = "X-Header-One";
        public static final String EXPOSE_HEADER = "X-Header-Two";
        public static final String MAX_AGE = "100";
    }

    @Override
    public void configure() {
        httpPort(6111);
        csrf().disable().protector(CSRFProtector.Predefined.RANDOM);
        sessionMapper(new CookieAndHeaderSessionMapper(app().config()));
        cors()
                .allowOrigin(ALLOW_ORIGIN)
                .allowHeaders(ALLOW_HEADER)
                .exposeHeaders(EXPOSE_HEADER)
                .maxAge(Integer.parseInt(MAX_AGE));
    }

    public static void main(String[] args) throws Exception {
        System.out.println(System.getenv("act_env_gh636_conf"));
        System.setProperty("act.http.port", "6111");
        System.setProperty("act.cli.port", "6222");
        System.setProperty("act.i18n", "true");
        Act.start("ACTEST");
    }

}
