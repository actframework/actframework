package ghissues.gh820;

import org.osgl.util.C;

import java.util.List;

public class IntegerService implements BaseService<Integer> {
    @Override
    public List<Integer> produceList() {
        return C.list(1, 2, 3);
    }
}
