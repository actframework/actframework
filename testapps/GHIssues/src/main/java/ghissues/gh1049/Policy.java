package ghissues.gh1049;

import com.alibaba.fastjson.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.$;

public class Policy {
    public Integer policyId;
    public Integer clientId;
    public Integer productId;
    public String effectiveDate;
    public JSONObject needs;

    public DateTime effectiveDate() {
        if (null == effectiveDate) {
            return null;
        }
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        return fmt.parseDateTime(effectiveDate);
    }

    @Override
    public int hashCode() {
        return $.hc(getClass(), policyId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Policy) {
            Policy that = $.cast(obj);
            return $.eq(that.policyId, this.policyId);
        }
        return false;
    }

}
