package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.IO;

import java.io.File;

/**
 * Test `@ResponseStatus` annotation on direct return object
 */
@UrlContext("562")
public class GH562 extends GithubIssueBase {

    @LoadResource("bigfile")
    private File bigFile;

    @LoadResource("smallfile")
    private File smallFile;

    @GetAction("big")
    public File downloadBigFile() {
        System.out.println(IO.checksum(bigFile));
        return bigFile;
    }

    @GetAction("small")
    public File downloadSmallFile() {
        System.out.println(IO.checksum(smallFile));
        return smallFile;
    }

    @PostAction("big")
    public File bigFileBlocking(String ignore) {
        return bigFile;
    }

    @PostAction("small")
    public File smallFileBlocking(String ignore) {
        return smallFile;
    }

    @PostAction("primitive")
    public boolean blockingPrimitive(String ignore) {
        return true;
    }

    @GetAction("primitive")
    public boolean getPrimitive() {
        return true;
    }
}
