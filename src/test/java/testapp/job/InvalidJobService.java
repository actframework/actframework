package testapp.job;

import act.job.OnAppStart;

public class InvalidJobService {
    @OnAppStart
    public void bar(String s) {
        System.out.println("bar");
    }
}
