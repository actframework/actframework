package act.view;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import act.app.ActionContext;
import com.google.zxing.BarcodeFormat;
import org.osgl.http.H;
import org.osgl.util.S;

public class QrCodeView extends View {

    private static class Render implements DirectRender {

        private BarcodeFormat fmt;

        Render(BarcodeFormat fmt) {
            this.fmt = fmt;
        }

        @Override
        public void render(Object result, ActionContext context) {
            new ZXingResult(S.string(result), fmt).apply(context.req(), context.resp());
        }
    }

    private static final Render QR_RENDER = new Render(BarcodeFormat.QR_CODE);
    private static final Render BARCODE_RENDER = new Render(BarcodeFormat.CODE_128);

    @Override
    public String name() {
        return "qrcode";
    }

    @Override
    protected Template loadTemplate(String resourcePath) {
        return null;
    }

    @Override
    protected Template loadInlineTemplate(String content) {
        return null;
    }

    @Override
    public DirectRender directRenderFor(H.Format acceptType) {
        if (S.eq(acceptType.name(), "qrcode", S.IGNORECASE)) {
            return QR_RENDER;
        } else if (S.eq(acceptType.name(), "barcode", S.IGNORECASE)) {
            return BARCODE_RENDER;
        }
        return null;
    }

}
