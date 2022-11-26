package ghissues;

import act.Act;
import act.apidoc.ApiManager;

@SuppressWarnings("unused")
public class AppEntry {

    private static void enableApiManager() {
        System.setProperty(ApiManager.SYS_PROP_ENABLE_API_MANAGER_ON_TEST_MODE, "true");
    }

    public static void main(String[] args) throws Exception {
        enableApiManager();
        Act.start();
    }

}
