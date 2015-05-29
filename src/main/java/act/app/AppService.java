package act.app;

import act.Destroyable;

public interface AppService<T extends AppService> extends Destroyable, AppHolder<T> {
}
