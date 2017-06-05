package testapp.endpoint.ghissues;

import act.Act;
import act.job.OnAppStart;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;

import javax.inject.Singleton;

@Singleton
public class GH222 extends GithubIssueBase {

    private long syncThreadId;
    private long asyncThreadId;

    @OnAppStart
    public void syncCallback() {
        syncThreadId = Thread.currentThread().getId();
    }

    @OnAppStart(async = true)
    public void asyncCallback() {
        asyncThreadId = Thread.currentThread().getId();
    }

    @GetAction("222")
    public void check() {
        Act.LOGGER.info(S.concat("sync: ", S.string(syncThreadId), "\tasync: ", S.string(asyncThreadId)));
        if (syncThreadId == asyncThreadId) {
            throw new RuntimeException("failed");
        }
    }

}
