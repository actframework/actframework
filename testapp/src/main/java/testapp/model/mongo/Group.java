package testapp.model.mongo;

import act.storage.Store;
import org.osgl.storage.impl.SObject;

import java.util.List;

public class Group {
    private String name;
    @Store
    private List<SObject> photos;
}
