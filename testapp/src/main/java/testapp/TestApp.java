package testapp;

import act.app.conf.AppConfigurator;
import act.boot.app.RunApp;

/**
 * This class runs an ActFramework application with public endpoints to be
 * tested
 */
public class TestApp extends AppConfigurator<TestApp> {

    @Override
    public void configure() {
        httpPort(6111);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("act.http.port", "6111");
        System.setProperty("act.cli.port", "6222");
        //System.setProperty("app.mode", "prod");
        RunApp.start("ACTEST", "0.1", TestApp.class);
    }
}
