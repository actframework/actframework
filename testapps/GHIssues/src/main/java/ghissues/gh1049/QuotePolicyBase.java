package ghissues.gh1049;

import act.data.annotation.Data;

@Data(callSuper = false)
public class QuotePolicyBase extends UcModelBase {
    public Integer clientId;
    public Integer productId;
    public String clientName;
    public String effectiveDate;
    public String needs;
}