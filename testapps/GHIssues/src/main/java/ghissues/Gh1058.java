package ghissues;

import act.controller.annotation.UrlContext;
import act.metric.MetricAdmin;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Inject;

@UrlContext("1058")
public class Gh1058 extends BaseController {

    @Inject
    MetricAdmin metricAdmin;

    @GetAction
    public Object metric() {
        return metricAdmin.getTimers(1, false, 2, null, false);
    }

}
