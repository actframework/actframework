package act.util;

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
import act.app.App;
import org.apache.commons.fileupload.FileItemStream;
import org.osgl.$;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.storage.KeyGenerator;
import org.osgl.storage.KeyNameProvider;
import org.osgl.storage.impl.FileSystemService;
import org.osgl.storage.impl.SObject;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.*;
import java.util.Map;

public class UploadFileStorageService extends FileSystemService {

    public static final KeyNameProvider ACT_STORAGE_KEY_NAME_PROVIDER = new KeyNameProvider() {
        @Override
        public String newKeyName() {
            return Act.cuid();
        }
    };

    private int inMemoryCacheThreshold;

    public UploadFileStorageService(Map<String, String> conf, int inMemoryCacheThreshold) {
        super(conf);
        this.setKeyNameProvider(ACT_STORAGE_KEY_NAME_PROVIDER);
        this.inMemoryCacheThreshold = inMemoryCacheThreshold;
    }

    public static UploadFileStorageService create(App app) {
        File tmp = app.tmpDir();
        if (!tmp.exists() && !tmp.mkdirs()) {
            throw E.unexpected("Cannot create tmp dir");
        }
        Map<String, String> conf = C.newMap("storage.fs.home.dir", Files.file(app.tmpDir(), "uploads").getAbsolutePath(),
                "storage.keygen", KeyGenerator.Predefined.BY_DATE.name());
        conf.put(IStorageService.CONF_ID, "__upload");
        conf.put("storage.storeSuffix", "false");
        return new UploadFileStorageService(conf, app.config().uploadInMemoryCacheThreshold());
    }

    public static ISObject store(FileItemStream fileItemStream, App app) {
        UploadFileStorageService ss = app.uploadFileStorageService();
        try {
            return ss._store(fileItemStream);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    private ISObject _store(FileItemStream fileItemStream) throws IOException {
        String filename = fileItemStream.getName();
        String key = newKey(filename);
        File tmpFile = getFile(key);
        InputStream input = fileItemStream.openStream();
        ThresholdingByteArrayOutputStream output = new ThresholdingByteArrayOutputStream(inMemoryCacheThreshold, tmpFile);
        IO.copy(input, output);

        ISObject retVal;
        if (output.exceedThreshold) {
            retVal = getFull(key);
        } else {
            int size = output.written;
            if (0 == size) {
                return null;
            }
            byte[] buf = output.buf();
            retVal = SObject.of(key, buf, size);
        }

        if (S.notBlank(filename)) {
            retVal.setFilename(filename);
        }
        String contentType = fileItemStream.getContentType();
        if (null != contentType) {
            retVal.setContentType(contentType);
        }
        return retVal;
    }

    private String newKey(String filename) {
        if (S.blank(filename)) {
            return S.concat(Act.cuid(), "tmp");
        }
        return S.pathConcat(getKey(Act.cuid()), '/', filename);
    }

    /**
     * The idea come from apache-commons-IO's `ThresholdingOutputStream`
     *
     * Before threshold exceeded the data is written into internal byte array buffer, once
     * threshold is reached then the internal byte array buffer will be dumped into the
     * second output stream and remaining written will be redirected to the second output
     * stream also
     */
    private static class ThresholdingByteArrayOutputStream extends ByteArrayOutputStream {
        private int threshold;
        private int written;
        private boolean exceedThreshold;
        private File file;
        private OutputStream fileOutputStream;

        public ThresholdingByteArrayOutputStream(int threshold, File file) {
            if (threshold < 1024) {
                threshold = 1024;
            }
            buf = new byte[threshold];
            this.threshold = threshold;
            this.file = $.requireNotNull(file);
        }

        @Override
        public synchronized void write(int b) {
            if (!checkThresholding(1)) {
                super.write(b);
            } else {
                try {
                    fileOutputStream.write(b);
                } catch (IOException e) {
                    throw E.ioException(e);
                }
            }
            written++;
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            if (!checkThresholding(len)) {
                super.write(b, off, len);
            } else {
                try {
                    fileOutputStream.write(b, off, len);
                } catch (IOException e) {
                    throw E.ioException(e);
                }
            }
            written += len;
        }

        @Override
        public void flush() throws IOException {
            if (exceedThreshold) {
                fileOutputStream.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (exceedThreshold) {
                IO.close(fileOutputStream);
            }
        }

        byte[] buf() {
            return this.buf;
        }

        private boolean checkThresholding(int bytes) {
            if (!exceedThreshold && (written + bytes > threshold)) {
                exceedThreshold = true;
                fileOutputStream = createFileOutputStream();
                try {
                    fileOutputStream.write(buf, 0, written);
                } catch (IOException e) {
                    throw E.ioException(e);
                }
            }
            return exceedThreshold;
        }

        private OutputStream createFileOutputStream() {
            File dir = file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw E.ioException("Cannot create dir: " + dir.getAbsolutePath());
            }
            return IO.buffered(IO.outputStream(file));
        }
    }

}
