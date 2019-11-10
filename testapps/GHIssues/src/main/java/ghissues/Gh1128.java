package ghissues;

import act.controller.annotation.UrlContext;
import act.storage.StorageServiceManager;
import com.alibaba.fastjson.JSONObject;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.storage.ISObject;
import org.osgl.storage.IStorageService;

@UrlContext("1128")
public class Gh1128 extends BaseController {

    @PostAction
    public void upload(ISObject file, StorageServiceManager ssMgr) {
        IStorageService ss = ssMgr.storageService("1128_upload");
        String key = ss.getKey();
        file.getAttribute(ISObject.ATTR_FILE_NAME);
        file = ss.put(key, file);
    }

}
