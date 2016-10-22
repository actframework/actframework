package testapp.endpoint;

import act.app.DbServiceManager;
import act.controller.Controller;
import act.db.morphia.MorphiaService;
import act.util.PropertySpec;
import org.osgl.mvc.annotation.DeleteAction;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.util.E;
import testapp.model.mongo.Person;

import javax.inject.Inject;

public class MorphiaTestBed extends Controller.Util {

    @Inject
    Person.Dao personDao;

    @DeleteAction("/morphia")
    public void cleanDb(DbServiceManager dbServiceManager) {
        MorphiaService svc = dbServiceManager.dbService(DbServiceManager.DEFAULT);
        svc.ds().getDB().dropDatabase();
    }

    @PostAction("/morphia/person")
    @PropertySpec("-photo")
    public void createPerson(Person person) {
        E.illegalStateIf(personDao.exists(person.getName()));
        personDao.save(person);
    }

    @GetAction("/morphia/person/{name}")
    public Person findPerson(String name) {
        return personDao.findOneBy("name", name);
    }

}
