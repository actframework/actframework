package ghissues;

import act.aaa.model.UserBase;

import javax.persistence.*;

@Entity(name = "user0")
@Table(name = "user0")
public class User extends UserBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
}
