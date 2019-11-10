package ghissues.gh1072;

import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

import javax.persistence.Entity;

@Entity
public class SupportInfo extends BaseModel {

    public boolean supported;

    @UrlContext("support")
    public static class Dao extends BaseModel.Dao<SupportInfo> {

        @GetAction
        public String test() {
            return "1072";
        }

    }

}
