package liusheng.controller;

import act.controller.annotation.Port;
import org.osgl.mvc.annotation.GetAction;

@Port({"port_a"})
public class HelloScanController {
    @GetAction("/hello")
    public String hello5() {

        return "SUCCESS";
    }
}
