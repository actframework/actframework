package actissue;

import javax.persistence.*;

@Entity(name = "proj")
@UniqueConstraint(columnNames = "name")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    public String name;

}
