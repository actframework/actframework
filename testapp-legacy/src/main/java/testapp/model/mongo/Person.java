package testapp.model.mongo;

import act.db.morphia.MorphiaDao;
import act.db.morphia.MorphiaModel;
import act.storage.Store;
import org.mongodb.morphia.annotations.Indexed;
import org.osgl.storage.impl.SObject;

/**
 * A person with name and photo
 */
public class Person extends MorphiaModel<Person> {

    @Indexed(unique = true)
    private String name;

    @Store
    private transient SObject photo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SObject getPhoto() {
        return photo;
    }

    public String getPhotoUrl() {
        return null == photo ? null : photo.getUrl();
    }

    public void setPhoto(SObject photo) {
        this.photo = photo;
    }

    public static class Dao extends MorphiaDao<Person> {

        public boolean exists(String name) {
            return countBy("name", name) > 0;
        }

    }


}
