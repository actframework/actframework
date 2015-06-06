package testapp.job;

import act.job.OnAppStart;

public class MyJobService {

    @OnAppStart
    public void foo() {
        System.out.println("foo");
    }

}
