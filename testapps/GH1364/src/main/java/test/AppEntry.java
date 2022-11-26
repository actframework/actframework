package test;

import act.Act;
import act.controller.Controller;
import act.util.JsonView;
import org.joda.time.LocalDate;
import org.osgl.mvc.annotation.GetAction;


@SuppressWarnings("unused")
@JsonView
public class AppEntry extends Controller.Util {

    @GetAction
    public LocalDate test(LocalDate date) {
        return date;
    }


    public static void main(String[] args) throws Exception {
        Act.start();
    }

}
