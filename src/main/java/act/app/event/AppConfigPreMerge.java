package act.app.event;

import act.app.App;

/**
 * Emitted right before merging {@link act.app.conf.AppConfigurator app custom config}
 */
public class AppConfigPreMerge extends AppEvent {
    public AppConfigPreMerge(App source) {
        super(AppEventId.CONFIG_PREMERGE, source);
    }
}
