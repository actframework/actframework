package gh918;

import act.Act;
import org.osgl.mvc.annotation.GetAction;

public class AppEntry {

    @GetAction
    public void test() {
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }
}
