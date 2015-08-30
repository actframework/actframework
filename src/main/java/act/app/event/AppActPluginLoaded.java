package act.app.event;

import act.app.App;

/**
 * Emitted immediately when {@link App}'s {@link act.plugin.Plugin}(s) has been loaded
 */
public class AppActPluginLoaded extends AppEvent {
    public AppActPluginLoaded(App source) {
        super(AppEventId.APP_ACT_PLUGIN_LOADED, source);
    }
}