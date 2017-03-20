package act.view;

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

import act.cli.view.CliView;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.http.H;
import org.osgl.mvc.result.RenderContent;

/**
 * An enhanced version of {@link org.osgl.mvc.result.RenderXML} that
 * allows {@link PropertySpec} to be applied to control the
 * output fields
 */
public class FilteredRenderXML extends RenderContent {

    public FilteredRenderXML(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(render(v, spec, context), H.Format.XML);
    }

    public FilteredRenderXML(H.Status status, Object v, PropertySpec.MetaInfo spec, ActContext context) {
        super(status, render(v, spec, context), H.Format.XML);
    }

    private static String render(Object v, PropertySpec.MetaInfo spec, ActContext context) {
        return CliView.XML.render(v, spec, context);
    }
}
