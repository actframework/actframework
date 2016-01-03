package act.data;

import act.TestBase;
import org.junit.Before;
import org.junit.Test;
import testapp.model.Person;

import java.util.List;

public class DataPropertyRepositoryTest extends TestBase {
    private DataPropertyRepository repo;

    @Before
    public void prepare() throws Exception {
        super.setup();
        repo = new DataPropertyRepository(mockApp);
    }

    @Test
    public void test() {
        List<String> ls = repo.propertyListOf(Person.class);
        yes(ls.contains("firstName"));
        yes(ls.contains("lastName"));
        yes(ls.contains("age"));
        yes(ls.contains("address.streetNo"));
        yes(ls.contains("address.streetName"));
        yes(ls.contains("address.city"));
    }

}
