package de.verdox.voxel.shared.data.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class Registry<T> {
    private final Map<ResourceLocation, T> keyToDataMapping = new HashMap<>();
    private final Map<T, ResourceLocation> dataToKeyMapping = new HashMap<>();

    public void register(T data, ResourceLocation location) {
        if (keyToDataMapping.containsKey(location)) {
            throw new IllegalArgumentException(location + " already used in registry");
        }
        if (dataToKeyMapping.containsKey(data)) {
            throw new IllegalArgumentException(data + " already registered in registry");
        }

        keyToDataMapping.put(location, data);
        dataToKeyMapping.put(data, location);
    }

    public T get(ResourceLocation resourceLocation) {
        return keyToDataMapping.getOrDefault(resourceLocation, null);
    }

    public T getOrThrow(ResourceLocation resourceLocation) {
        return Objects.requireNonNull(get(resourceLocation), "No entry found for key " + resourceLocation);
    }

    public Optional<T> getOptional(ResourceLocation resourceLocation) {
        return Optional.ofNullable(get(resourceLocation));
    }

    public ResourceLocation getKey(T data) {
        return dataToKeyMapping.getOrDefault(data, null);
    }

    public ResourceLocation getKeyOrThrow(T data) {
        return Objects.requireNonNull(getKey(data), "No key found for " + data);
    }

    public Stream<T> streamEntries() {
        return keyToDataMapping.values().stream();
    }

    public Stream<ResourceLocation> streamKeys() {
        return keyToDataMapping.keySet().stream();
    }

    public Stream<Map.Entry<ResourceLocation,T>> stream() {
        return keyToDataMapping.entrySet().stream();
    }

    public int getAmountValues() {
        return keyToDataMapping.size();
    }
}
