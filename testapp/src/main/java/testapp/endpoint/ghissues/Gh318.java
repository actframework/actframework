package testapp.endpoint.ghissues;

import act.cli.Command;
import act.db.morphia.MorphiaAdaptiveRecord;
import act.db.morphia.MorphiaDao;
import act.inject.param.NoBind;
import org.mongodb.morphia.annotations.Entity;

import java.util.List;

@Entity("gh318")
public class Gh318 extends MorphiaAdaptiveRecord<Gh318> {

    public static class Dao extends MorphiaDao<Gh318> {

        @NoBind
        private List<String> list;

        /*
         * invoke `gh318.test any-str`
         */
        @Command("gh318.test")
        public String show(int id, boolean b, String s) {
            return "" + id + b + s;
        }
    }
}
