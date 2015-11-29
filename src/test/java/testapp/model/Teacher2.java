package testapp.model;

import act.util.AutoObject;

/**
 * Created by luog on 28/11/15.
 */
@AutoObject(callSuper = false)
public class Teacher2 extends Person2 {
    private String teacherId;
    public Teacher2(String fn, String ln, Address2 addr, Integer age, String teacherId) {
        super(fn, ln, addr, age);
        this.teacherId = teacherId;
    }

    public String getTeacherId() {
        return teacherId;
    }

}
