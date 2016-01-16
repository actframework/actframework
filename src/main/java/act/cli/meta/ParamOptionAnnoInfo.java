package act.cli.meta;

/**
 * Store {@link OptionAnnoInfoBase option annotation info} that
 * come from a method parameter
 */
public class ParamOptionAnnoInfo extends OptionAnnoInfoBase {
    private int index;

    public ParamOptionAnnoInfo(int index, boolean optional) {
        super(optional);
        this.index = index;
    }

    public int index() {
        return index;
    }

}
