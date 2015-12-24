package testapp.model;

import act.util.Data;
import org.osgl.$;

/**
 * Created by luog on 28/11/15.
 */
@Data(callSuper = false)
public class Teacher extends Person {
    private String teacherId;
    public Teacher(String fn, String ln, Address addr, Integer age, String teacherId) {
        super(fn, ln, addr, age);
        this.teacherId = teacherId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    @Override
    public int hashCode() {
        return $.hc(teacherId);
    }
}
