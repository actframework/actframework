package testapp.endpoint.ghissues.gh554;

import act.data.annotation.Data;
import act.util.SimpleBean;
import org.osgl.util.S;

@Data
public class MsgTemplate implements SimpleBean {
    public String id;

    public MsgTemplate(String id) {
        this.id = id;
    }
}
