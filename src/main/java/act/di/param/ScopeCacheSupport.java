package act.di.param;

public interface ScopeCacheSupport {

    <T> T get(String key);

    <T> void put(String key, T t);

}
