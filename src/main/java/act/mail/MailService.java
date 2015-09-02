package act.mail;

import act.ActComponent;
import act.app.App;
import act.app.AppServiceBase;
import org.osgl.util.E;

@ActComponent
public class MailService extends AppServiceBase<MailService> {

    private String id;


    public MailService(String id, App app) {
        super(app);
        E.NPE(id);
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    protected void releaseResources() {

    }
}
