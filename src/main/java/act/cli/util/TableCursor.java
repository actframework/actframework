package act.cli.util;

import act.cli.CliContext;
import act.cli.view.CliView;
import act.util.PropertySpec;
import org.osgl.util.C;

import java.util.List;

/**
 * Used to paginate table layout
 */
public class TableCursor implements CliCursor {

    private List data;
    private int pageSize;
    private int pageNo;
    private PropertySpec.MetaInfo propertySpec;

    public TableCursor(List data, int pageSize, PropertySpec.MetaInfo propertySpec) {
        this.data = data;
        this.pageSize = pageSize;
        this.propertySpec = propertySpec;
    }

    private List get() {
        int history = pageSize * pageNo++;
        if (history >= data.size()) {
            return C.list();
        }
        return C.list(data).drop(history).take(pageSize);
    }

    @Override
    public boolean hasNext() {
        return pageSize * pageNo < data.size();
    }

    @Override
    public void output(CliContext context) {
        List list = get();
        if (list.isEmpty()) {
            context.session().removeCursor();
            context.println("no cursor");
            return;
        } else {
            CliView.TABLE.print(list, propertySpec, context);
        }
    }

    @Override
    public int records() {
        return data.size();
    }
}
