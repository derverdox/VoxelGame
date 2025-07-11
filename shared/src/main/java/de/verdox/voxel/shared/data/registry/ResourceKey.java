package de.verdox.voxel.shared.data.registry;

public record ResourceKey<T>(ResourceKey<Registry<T>> registryKey, ResourceLocation resource) {

}
