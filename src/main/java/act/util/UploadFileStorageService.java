package act.util;

import act.app.ActionContext;
import act.app.App;
import act.handler.builtin.AlwaysNotFound;
import act.handler.builtin.controller.FastRequestHandler;
import org.apache.commons.fileupload.FileItem;
import org.osgl.http.H;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.storage.KeyGenerator;
import org.osgl.storage.impl.FileSystemService;
import org.osgl.storage.impl.SObject;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

public class UploadFileStorageService extends FileSystemService {

    public UploadFileStorageService(Map<String, String> conf) {
        super(conf);
    }

    public static IStorageService create(App app) {
        File tmp = app.tmpDir();
        if (!tmp.exists() && !tmp.mkdirs()) {
            throw E.unexpected("Cannot create tmp dir");
        }
        Map<String, String> conf = C.newMap("storage.fs.home.dir", Files.file(app.tmpDir(), "uploads").getAbsolutePath(),
                "storage.keygen", KeyGenerator.BY_DATE.name());
        conf.put(IStorageService.CONF_ID, "__upload");
        return new UploadFileStorageService(conf);
    }

    public static ISObject store(FileItem file, App app) {
        IStorageService ss = app.uploadFileStorageService();
        try {
            String key = newKey();
            ISObject sobj = SObject.of(file.getInputStream());
            sobj.setAttribute(SObject.ATTR_FILE_NAME, file.getName());
            sobj.setAttribute(SObject.ATTR_CONTENT_TYPE, file.getContentType());
            sobj.setAttribute(SObject.ATTR_URL, "/~upload/" + sobj.getKey());
            ss.put(key, sobj);
            return ss.get(key);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static String newKey() {
        // Note we don't use App.cuid() here to get higher level security
        return UUID.randomUUID().toString();
    }

    public static class UploadFileGetter extends FastRequestHandler {

        @Override
        public void handle(ActionContext context) {
            String key = context.paramVal("path");
            if (S.blank(key)) {
                AlwaysNotFound.INSTANCE.handle(context);
                return;
            }
            ISObject sobj = context.app().uploadFileStorageService().get(key);
            if (null == sobj) {
                AlwaysNotFound.INSTANCE.handle(context);
                return;
            }
            H.Format fmt = H.Format.of(sobj.getAttribute(ISObject.ATTR_CONTENT_TYPE));
            InputStream is = new BufferedInputStream(sobj.asInputStream());
            H.Response resp = context.resp();
            if (null != fmt && H.Format.UNKNOWN != fmt) {
                resp.contentType(fmt.contentType());
            }
            IO.copy(is, context.resp().outputStream());
        }

        @Override
        public String toString() {
            return "Upload file getter";
        }
    }
}
