package ghissues;

import act.controller.annotation.UrlContext;
import act.inject.util.LoadResource;
import act.util.JsonView;
import act.util.LogSupport;
import ghissues.gh790.Student;
import org.osgl.mvc.annotation.GetAction;

import java.util.List;
import java.util.Map;

@UrlContext("790")
@JsonView
public class Gh790 extends LogSupport {

    @LoadResource("students.xls")
    public List<Student> students;

    @LoadResource("students.xls")
    public Map<String, List<Student>> studentByClass;

    @GetAction("students")
    public List<Student> all() {
        return students;
    }

    @GetAction("info/classNumber")
    public int classNumber() {
        return studentByClass.keySet().size();
    }

}
