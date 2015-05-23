package org.osgl.oms.di;

public interface DependencyInjector<FT extends DependencyInjector> {
    /**
     * Create an instance of type T using the class of type T
     * @param clazz
     * @param <T>
     * @return the instance
     */
    <T> T create(Class<T> clazz);
}
