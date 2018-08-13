package testapp.endpoint.ghissues.gh310;

import act.db.morphia.MorphiaModel;
import org.mongodb.morphia.annotations.Entity;

@Entity("gh301")
public class GH310Model extends MorphiaModel<GH310Model> {
    public String name;
}
