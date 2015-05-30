package act.util;

import act.app.App;
import org.apache.commons.fileupload.FileItem;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;
import org.osgl.storage.KeyGenerator;
import org.osgl.storage.impl.FileSystemService;
import org.osgl.storage.impl.SObject;
import org.osgl.util.C;
import org.osgl.util.E;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class UploadFileStorageService extends FileSystemService {
    public UploadFileStorageService(Map<String, String> conf) {
        super(conf);
    }

    public static IStorageService create(App app) {
        Map<String, String> conf = C.newMap("storage.fs.home.dir", Files.file(app.tmpDir(), "uploads").getAbsolutePath(),
                "storage.keygen", KeyGenerator.BY_DATE.name());
        IStorageService ss = new UploadFileStorageService(conf);
        return ss;
    }

    public static ISObject store(FileItem file, App app) {
        IStorageService ss = app.uploadFileStorageService();
        try {
            ISObject sobj = SObject.of(file.getInputStream());
            sobj.setAttribute(SObject.ATTR_FILE_NAME, file.getName());
            sobj.setAttribute(SObject.ATTR_CONTENT_TYPE, file.getContentType());
            String key = newKey();
            ss.put(key, sobj);
            return ss.get(key);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static String newKey() {
        return UUID.randomUUID().toString();
    }
}
