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

import act.Act;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.result.Result;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.Map;

public class ZXingResult extends Result {

    private BarcodeFormat barcodeFormat;
    private ErrorCorrectionLevel errorCorrectionLevel;
    private int width;
    private int height;

    public ZXingResult(String content) {
        this(content, BarcodeFormat.QR_CODE);
    }

    public ZXingResult(String content, BarcodeFormat barcodeFormat) {
        this(content, barcodeFormat, null);
    }

    public ZXingResult(String content, BarcodeFormat barcodeFormat, ErrorCorrectionLevel errorCorrectionLevel) {
        super(H.Status.OK, content);
        this.barcodeFormat = $.requireNotNull(barcodeFormat);
        this.errorCorrectionLevel = errorCorrectionLevel;
        setDefaultDimension();
    }

    public ZXingResult width(int width) {
        this.width = width;
        return this;
    }

    public ZXingResult height(int height) {
        this.height = height;
        return this;
    }

    public ZXingResult errorCorrectionLevel(ErrorCorrectionLevel level) {
        this.errorCorrectionLevel = level;
        return this;
    }

    public ZXingResult barCodeFormat(BarcodeFormat format) {
        this.barcodeFormat = format;
        return this;
    }

    @Override
    protected void applyMessage(H.Request request, H.Response response) {
        String msg = this.getMessage();
        this.applyBeforeCommitHandler(request, response);
        if(S.notBlank(msg)) {
            renderCode(response);
        } else {
            IO.close(response.outputStream());
        }

        this.applyAfterCommitHandler(request, response);
    }

    protected ErrorCorrectionLevel errorCorrectionLevel() {
        return errorCorrectionLevel;
    }

    protected BarcodeFormat barcodeFormat() {
        return barcodeFormat;
    }

    private void setDefaultDimension() {
        if (barcodeFormat() == BarcodeFormat.QR_CODE) {
            width = 128;
            height = 128;
        } else {
            width = 128;
            height = 96;
        }
    }

    private void renderCode(H.Response response) {
        response.contentType("image/png");
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, Act.appConfig().encoding());
        hints.put(EncodeHintType.MARGIN, 0);
        ErrorCorrectionLevel level = errorCorrectionLevel();
        if (null != level) {
            hints.put(EncodeHintType.ERROR_CORRECTION, level);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = writer.encode(getMessage(), barcodeFormat(), width, height, hints);
            MatrixToImageWriter.writeToStream(bitMatrix, "png", response.outputStream());
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public static ZXingResult barcode(String content) {
        return new ZXingResult(content, BarcodeFormat.CODE_128);
    }

    public static ZXingResult qrcode(String content) {
        return new ZXingResult(content, BarcodeFormat.QR_CODE);
    }

}
