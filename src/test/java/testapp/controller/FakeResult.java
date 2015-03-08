package testapp.controller;

import org.osgl.http.H;
import org.osgl.mvc.result.Result;

import java.util.Arrays;

public class FakeResult extends Result {
    Object[] args;
    protected FakeResult(Object... args) {
        super(H.Status.OK);
        this.args = args;
    }

    @Override
    public String toString() {
        return Arrays.toString(args);
    }
}
