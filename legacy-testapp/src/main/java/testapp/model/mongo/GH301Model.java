package testapp.model.mongo;

import act.db.morphia.MorphiaAdaptiveRecord;
import org.mongodb.morphia.annotations.Entity;

/**
 * Used to test GH301 issue
 */
@Entity("gh301")
public class GH301Model extends MorphiaAdaptiveRecord<GH301Model> {
}
