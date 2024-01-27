package services;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ServiceLocator {
    private static ServiceLocator instance = new ServiceLocator();

    public ServiceLocator() {
        instance = this;
    }

    private final Map<Class<?>, Service> services = new HashMap<>();

    public<TBase extends Service, T extends TBase> void register(
            Class<TBase> serviceClazz, T service) {
        services.put(serviceClazz, service);
    }

    public<TBase extends Service> void unregister(Class<TBase> serviceClazz) {
        services.remove(serviceClazz);
    }

    @SuppressWarnings("unchecked")
    public static<T extends Service> T get(Class<T> serviceClazz) {
        return (T) instance.services.get(serviceClazz);
    }
}
