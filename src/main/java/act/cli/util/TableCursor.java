package act.cli.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
