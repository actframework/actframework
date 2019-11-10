package test;

import act.db.jpa.JPADao;

import javax.persistence.*;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String name;

    public static class Dao extends JPADao<Integer, User> {
    }


}