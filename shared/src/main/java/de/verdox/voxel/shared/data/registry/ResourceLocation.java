package de.verdox.voxel.shared.data.registry;

import java.util.regex.Pattern;

import java.util.Objects;

public class ResourceLocation {
    private static final String STANDARD_NAMESPACE = "voxel";
    private static final Pattern NAMESPACE_PATTERN =
        Pattern.compile("^[a-z0-9_.-]+$");
    private static final Pattern PATH_PATTERN =
        Pattern.compile("^[a-z0-9/._-]+$");

    private final String namespace;
    private final String path;

    private ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation of(String path) {
        return of(STANDARD_NAMESPACE, path);
    }

    public static ResourceLocation of(String namespace, String path) {
        Objects.requireNonNull(namespace, "Namespace cannot be null");
        Objects.requireNonNull(path, "Path cannot be null");

        if (namespace.isEmpty()) {
            namespace = STANDARD_NAMESPACE;
        }

        if (!NAMESPACE_PATTERN.matcher(namespace).matches()) {
            throw new IllegalArgumentException(
                "Error in namespace: " + namespace
            );
        }
        if (!PATH_PATTERN.matcher(path).matches()) {
            throw new IllegalArgumentException(
                "Error in path: " + path
            );
        }

        // 4) Erzeugen der ResourceLocation
        return new ResourceLocation(namespace, path);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ResourceLocation that = (ResourceLocation) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }
}
