package testapp.model;

import act.util.Data;

/**
 * Created by luog on 28/11/15.
 */
@Data(callSuper = true)
public class Student2 extends Person2 {
    private String clazz;
    private String studentId;
    private double score;
    private int[] ia;
    public Student2(String fn, String ln, Address2 addr, Integer age, String clazz, String studentId, Double score) {
        super(fn, ln, addr, age);
        this.clazz = clazz;
        this.studentId = studentId;
        this.score = score;
        this.ia = new int[]{1, 2, 3};
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

}
