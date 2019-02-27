package test;

import act.Act;
import act.job.OnAppStart;

public class AppEntry {

    @OnAppStart
    public void onAppStart(User.Dao userDao) {
        User user = new User();
        user.name = "tom";
        userDao.save(user);
    }

    public static void main(String[] args) throws Exception{
        Act.start();
    }
}
