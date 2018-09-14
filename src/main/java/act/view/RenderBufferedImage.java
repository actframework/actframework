package act.view;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import static org.osgl.http.H.Header.Names.CONTENT_DISPOSITION;

import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.util.*;

import java.awt.image.BufferedImage;

public class RenderBufferedImage extends Result {

    private final BufferedImage img;

    private final String contentType;

    public RenderBufferedImage(BufferedImage img) {
        this(img, "image/png");
    }

    public RenderBufferedImage(BufferedImage img, String contentType) {
        this.img = $.requireNotNull(img);
        this.contentType = S.requireNotBlank(contentType);
    }

    @Override
    public void apply(H.Request req, H.Response resp) {
        try {
            applyCookies(resp);
            applyHeaders(resp);
            resp.contentType(contentType);
            if (!resp.containsHeader(CONTENT_DISPOSITION)) {
                resp.contentDisposition(null, true);
            }
            applyStatus(resp);
            applyBeforeCommitHandler(req, resp);
            Img.source(img).writeTo(resp.outputStream(), contentType);
        } catch (Exception e) {
            throw E.unexpected(e);
        } finally {
            try {
                resp.commit();
                applyAfterCommitHandler(req, resp);
            } finally {
                clearThreadLocals();
            }
        }
    }

}
